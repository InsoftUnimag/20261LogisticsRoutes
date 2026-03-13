# Feature Specification: Planificación Automática de Rutas (MOD2-UC-001)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 1 – Planificar Rutas (Priority: P1)

**Como** Sistema de Gestión de Paquetes,  
**quiero** solicitar una ruta en el momento en que un paquete es registrado exitosamente (estado "recibido_en_sede"),  
**para que** el sistema agrupe el paquete con otros de la misma zona geográfica, gestione el tipo de vehículo requerido y lo tenga listo para despacho dentro del plazo establecido.

**Why this priority:** Es la integración crítica entre sistemas y el disparador de todo el flujo logístico. Sin ella, los paquetes no pueden ser asignados a una ruta y el flujo de despacho queda bloqueado.

**Independent Test:** Se puede probar enviando la solicitud de ruta para un paquete recién registrado en estado "recibido_en_sede" y verificando que el sistema retorna un `ruta_id` válido, agrupa el paquete en la ruta correcta según sus coordenadas GPS y actualiza el tipo de vehículo requerido si el peso acumulado lo exige. El paquete debe continuar su propio ciclo de vida en el Sistema de Gestión de Paquetes.

---

## Acceptance Scenarios

1. **Scenario:** Asignación a ruta existente con capacidad disponible
   - **Given:** Un paquete recién registrado en estado "recibido_en_sede" llega al sistema con sus coordenadas GPS y existe una ruta en estado "creada" cuya zona geográfica coincide.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema agrega el paquete a la ruta existente, actualiza el peso acumulado, recalcula el porcentaje de capacidad utilizada y retorna al Sistema de Gestión de Paquetes el `ruta_id`. El paquete continúa su ciclo de vida en el Sistema de Gestión de Paquetes.

2. **Scenario:** Creación de nueva ruta por zona sin ruta activa
   - **Given:** Un paquete recién registrado en estado "recibido_en_sede" llega al sistema y no existe ninguna ruta en estado "creada" cuya zona geográfica coincida con su destino.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema crea una nueva ruta para esa zona en estado "creada", asigna el tipo de vehículo más pequeño que soporte el peso inicial, registra la `fecha_creacion_ruta` y calcula la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días`. Retorna al Sistema de Gestión de Paquetes el `ruta_id`. El paquete continúa su ciclo de vida en el Sistema de Gestión de Paquetes.

3. **Scenario:** Reasignación de vehículo o marcado para despacho al alcanzar el 90% de capacidad
   - **Given:** Un paquete llega a una ruta en estado "creada" y al sumar su peso el total alcanza o supera el 90% de la capacidad del tipo de vehículo actualmente requerido por la ruta.
   - **When:** El sistema recalcula el peso acumulado tras agregar el paquete.
   - **Then:** El sistema evalúa si existe un tipo de vehículo de mayor capacidad en el orden establecido (Moto 20 kg → Van 500 kg → NHR 2.000 kg → Turbo 4.500 kg):
     - Si existe un tipo mayor: reasigna automáticamente al siguiente tipo, agrega el paquete y retorna al Sistema de Gestión de Paquetes el `ruta_id`. La ruta continúa en estado "creada" aceptando más paquetes.
     - Si ya es Turbo (capacidad máxima): agrega el paquete, la ruta transiciona a estado "lista_para_despacho" y el sistema notifica al Despachador Logístico indicando zona, número de paquetes, peso acumulado y tipo de vehículo requerido.

4. **Scenario:** Transición automática a 'Lista para Despacho' por vencimiento de plazo
   - **Given:** Una ruta se encuentra en estado "creada" y el sistema detecta que se ha alcanzado su `fecha_limite_despacho` (calculada como `fecha_creacion_ruta + 5 días`).
   - **When:** El sistema evalúa el estado de las rutas activas al llegar a la `fecha_limite_despacho`.
   - **Then:** El sistema transiciona la ruta automáticamente a estado "lista_para_despacho", independientemente del número de paquetes acumulados, y notifica al Despachador Logístico indicando zona, número de paquetes, peso acumulado, tipo de vehículo requerido y que la transición fue por vencimiento de plazo.

5. **Scenario:** Despacho manual anticipado por el Despachador Logístico
   - **Given:** Una ruta se encuentra en estado "creada" con al menos un paquete asignado y el Despachador Logístico decide que la ruta debe salir antes de que se cumplan las condiciones automáticas.
   - **When:** El Despachador Logístico solicita manualmente el despacho anticipado de la ruta.
   - **Then:** El sistema transiciona la ruta a estado "lista_para_despacho" y la pone disponible para que el Despachador asigne conductor y vehículo físico en el flujo de confirmación. (El flujo de confirmación se define en SPEC-02.)

---

## Edge Cases

- **¿Qué pasa si dos paquetes de la misma zona llegan simultáneamente y ambos intentan crear una ruta nueva al mismo tiempo?**  
  El sistema debe garantizar que solo se crea una ruta por zona en ese instante mediante control de concurrencia a nivel de base de datos. No puede haber rutas duplicadas por zona en estado "creada".

- **¿Qué ocurre si la `fecha_limite_entrega` del paquete ya venció en el momento en que el Sistema de Gestión de Paquetes envía la solicitud de ruta?**  
  El sistema descarta la solicitud sin crear ni modificar ninguna ruta, y notifica al Despachador Logístico con el detalle del paquete afectado para que tome la decisión correspondiente.

- **¿Qué ocurre si un paquete es asignado a una ruta planificada pero luego cae en estado "novedad_en_bodega" en el Sistema de Gestión de Paquetes?**  
  El sistema de rutas no recibe notificación automática de este estado. El paquete queda asociado a la ruta pero su ciclo de vida queda bloqueado en el Sistema de Gestión de Paquetes.

- **¿Qué pasa si se confirma una ruta con un paquete que aún no está en estado "listo_para_despacho"?**  
  El Despachador Logístico es responsable de verificar el estado físico de los paquetes antes de confirmar. El sistema no bloquea automáticamente la confirmación por este motivo, pero el Despachador puede excluir paquetes con novedades antes de confirmar.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-001** | El sistema DEBE recibir la solicitud de ruta del Sistema de Gestión de Paquetes cuando un paquete alcanza el estado "recibido_en_sede", incluyendo `paquete_id`, coordenadas GPS de destino, peso en kg y `fecha_limite_entrega`. |
| **FR-002** | El sistema DEBE agrupar paquetes en rutas según proximidad geográfica de sus coordenadas GPS de destino. |
| **FR-003** | El sistema DEBE asignar el tipo de vehículo más pequeño disponible que soporte el peso acumulado de la ruta al momento de su creación, comenzando siempre por Moto. |
| **FR-004** | El sistema DEBE reasignar automáticamente al siguiente tipo de vehículo cuando el peso acumulado supere la capacidad del tipo actual, siguiendo el orden: Moto (20kg) → Van (500kg) → NHR (2,000kg) → Turbo (4,500kg). |
| **FR-005** | El sistema DEBE calcular la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días` al crear una ruta nueva. |
| **FR-006** | El sistema DEBE transicionar automáticamente una ruta a estado 'Lista para Despacho' cuando se alcance su fecha_limite_despacho, independientemente del número de paquetes acumulados, y notificar al Despachador Logístico indicando que la transición fue por vencimiento de plazo. |
| **FR-007** | El sistema DEBE transicionar una ruta a estado 'Lista para Despacho' y notificar al Despachador Logístico cuando el peso acumulado alcance el 90% de la capacidad del Turbo, al no existir un tipo de vehículo de mayor capacidad. |
| **FR-008** | El sistema DEBE recibir y almacenar la `fecha_limite_entrega` de cada paquete para monitorear vencimientos y notificar al Despachador Logístico. |

---

## Key Entities

| Entidad | Atributos relevantes a esta historia |
|---|---|
| **Ruta** | `ruta_id`, `zona_geografica`, `estado` ("creada" / "lista_para_despacho" / "confirmada" / "en_transito" / "cerrada"), `peso_acumulado_kg`, `tipo_vehiculo_requerido`, `fecha_creacion_ruta`, `fecha_limite_despacho`, `lista_paquetes`. |
| **Tipo de Vehículo** | `tipo` (Moto / Van / NHR / Turbo), `capacidad_maxima_kg`, `unidades_disponibles`. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | El sistema asigna o genera una ruta para cada paquete en estado "recibido_en_sede" y retorna el `ruta_id` al Sistema de Gestión de Paquetes sin intervención manual. |
| **SC-002** | El sistema reasigna el tipo de vehículo correctamente cada vez que el peso acumulado supera la capacidad del tipo actual. |
| **SC-003** | El sistema transiciona la ruta a 'Lista para Despacho' y notifica al Despachador Logístico en menos de 5 segundos cuando el peso acumulado alcanza el 90% del Turbo. |
| **SC-004** | El sistema transiciona automáticamente a 'Lista para Despacho' toda ruta que alcance su fecha_limite_despacho sin importar el número de paquetes acumulados, y notifica al Despachador Logístico en menos de 5 segundos. |
| **SC-005** | El Despachador Logístico puede transicionar manualmente cualquier ruta en estado 'Creada' a 'Lista para Despacho' sin restricciones de capacidad ni tiempo. |
