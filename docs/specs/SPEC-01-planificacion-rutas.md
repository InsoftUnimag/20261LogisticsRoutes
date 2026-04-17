# Feature Specification: Planificación Automática de Rutas (MOD2-UC-001)

**Created:** 27/02/2026  
**Updated:** 19/03/2026 — Se incorpora la creación de Paradas como parte del flujo de planificación.  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 1 – Planificar Rutas (Priority: P1)

**Como** Sistema de Gestión de Paquetes,  
**quiero** solicitar una ruta en el momento en que un paquete es registrado exitosamente (estado "recibido_en_sede"),  
**para que** el sistema agrupe el paquete como una Parada dentro de una ruta de la misma zona geográfica, gestione el tipo de vehículo requerido y lo tenga listo para despacho dentro del plazo establecido.

**Why this priority:** Es la integración crítica entre sistemas y el disparador de todo el flujo logístico. Sin ella, los paquetes no pueden ser convertidos en Paradas dentro de una ruta y el flujo de despacho queda bloqueado.

**Independent Test:** Se puede probar enviando la solicitud de ruta para un paquete recién registrado en estado "recibido_en_sede" y verificando que: el sistema retorna un `ruta_id` válido, se crea una Parada en estado "pendiente" dentro de la ruta correcta según sus coordenadas GPS, y el tipo de vehículo requerido se actualiza si el peso acumulado lo exige. El paquete debe continuar su propio ciclo de vida en el Sistema de Gestión de Paquetes.

---

## Acceptance Scenarios

1. **Scenario:** Asignación a ruta existente — se crea Parada en ruta con capacidad disponible
    - **Given:** Un paquete recién registrado en estado "recibido_en_sede" llega al sistema con sus coordenadas GPS, peso y `fecha_limite_entrega`, y existe una ruta en estado "creada" cuya zona geográfica coincide y tiene capacidad disponible.
    - **When:** El sistema evalúa la asignación del paquete.
    - **Then:** El sistema crea una Parada en estado "pendiente" dentro de la ruta existente, con los datos del paquete (dirección, coordenadas, `paquete_id`, `fecha_limite_entrega`). Actualiza el peso acumulado de la ruta, recalcula el porcentaje de capacidad utilizada y retorna al Sistema de Gestión de Paquetes el `ruta_id`. El paquete continúa su ciclo de vida en el Sistema de Gestión de Paquetes.

2. **Scenario:** Creación de nueva ruta — se crea la ruta y la primera Parada
    - **Given:** Un paquete recién registrado en estado "recibido_en_sede" llega al sistema y no existe ninguna ruta en estado "creada" cuya zona geográfica coincida con su destino.
    - **When:** El sistema evalúa la asignación del paquete.
    - **Then:** El sistema crea una nueva ruta para esa zona en estado "creada", asigna el tipo de vehículo más pequeño que soporte el peso inicial (comenzando por Moto), registra la `fecha_creacion_ruta` y calcula la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días`. Acto seguido, crea la primera Parada en estado "pendiente" dentro de la nueva ruta con los datos del paquete (dirección, coordenadas, `paquete_id`, `fecha_limite_entrega`). Retorna al Sistema de Gestión de Paquetes el `ruta_id`. El paquete continúa su ciclo de vida en el Sistema de Gestión de Paquetes.

3. **Scenario:** Reasignación de vehículo al alcanzar el 90% de capacidad — se crea Parada y se escala el tipo de vehículo
    - **Given:** Un paquete llega a una ruta en estado "creada" y al sumar su peso el total alcanza o supera el 90% de la capacidad del tipo de vehículo actualmente requerido por la ruta, y existe un tipo de vehículo de mayor capacidad en la escala definida.
    - **When:** El sistema recalcula el peso acumulado tras agregar el paquete.
    - **Then:** El sistema crea la Parada en estado "pendiente" dentro de la ruta, reasigna automáticamente el tipo de vehículo requerido al siguiente en la escala (Moto 20 kg → Van 500 kg → NHR 2.000 kg → Turbo 4.500 kg) y retorna al Sistema de Gestión de Paquetes el `ruta_id`. La ruta continúa en estado "creada" aceptando más Paradas.

4. **Scenario:** Ruta llega a capacidad máxima del Turbo — se crea última Parada y la ruta pasa a "lista_para_despacho"
    - **Given:** Un paquete llega a una ruta en estado "creada" cuyo tipo de vehículo requerido ya es Turbo (capacidad máxima), y al sumar su peso el total alcanza o supera el 90% de la capacidad del Turbo.
    - **When:** El sistema recalcula el peso acumulado tras agregar el paquete.
    - **Then:** El sistema crea la Parada en estado "pendiente" dentro de la ruta, transiciona la ruta a estado "lista_para_despacho" y notifica al Despachador Logístico indicando zona, número de Paradas, peso acumulado y tipo de vehículo requerido. La ruta deja de aceptar nuevas Paradas.

5. **Scenario:** Transición automática a "lista_para_despacho" por vencimiento de plazo
    - **Given:** Una ruta se encuentra en estado "creada" con al menos una Parada en estado "pendiente", y el sistema detecta que se ha alcanzado su `fecha_limite_despacho`.
    - **When:** El sistema evalúa el estado de las rutas activas al llegar a la `fecha_limite_despacho`.
    - **Then:** El sistema transiciona la ruta automáticamente a estado "lista_para_despacho", independientemente del número de Paradas acumuladas, y notifica al Despachador Logístico indicando zona, número de Paradas, peso acumulado, tipo de vehículo requerido y que la transición fue por vencimiento de plazo.

6. **Scenario:** Despacho manual anticipado por el Despachador Logístico
    - **Given:** Una ruta se encuentra en estado "creada" con al menos una Parada en estado "pendiente" y el Despachador Logístico decide que la ruta debe salir antes de que se cumplan las condiciones automáticas.
    - **When:** El Despachador Logístico solicita manualmente el despacho anticipado de la ruta.
    - **Then:** El sistema transiciona la ruta a estado "lista_para_despacho" y la pone disponible para que el Despachador asigne conductor y vehículo físico en el flujo de confirmación. (El flujo de confirmación se define en MOD2-UC-002.)

---

## Edge Cases

- **¿Qué pasa si dos paquetes de la misma zona llegan simultáneamente y ambos intentan crear una ruta nueva al mismo tiempo?**  
  El sistema debe garantizar que solo se crea una ruta por zona en ese instante mediante control de concurrencia a nivel de base de datos. No puede haber rutas duplicadas por zona en estado "creada". El segundo paquete deberá esperar a que la ruta sea creada por el primero y entonces crear su Parada dentro de esa ruta.

- **¿Qué ocurre si la `fecha_limite_entrega` del paquete ya venció en el momento en que el Sistema de Gestión de Paquetes envía la solicitud de ruta?**  
  El sistema descarta la solicitud sin crear ninguna Parada ni modificar ninguna ruta, y notifica al Despachador Logístico con el detalle del paquete afectado para que tome la decisión correspondiente.

- **¿Qué ocurre si un paquete es asignado a una ruta (su Parada fue creada) pero luego cae en estado "novedad_en_bodega" en el Sistema de Gestión de Paquetes?**  
  El sistema de rutas no recibe notificación automática de este estado. La Parada queda en estado "pendiente" dentro de la ruta, pero el ciclo de vida del paquete queda bloqueado en el Sistema de Gestión de Paquetes. El Despachador podrá excluir esa Parada antes de confirmar el despacho (flujo definido en MOD2-UC-002).

- **¿Qué pasa si se confirma una ruta con una Parada cuyo paquete aún no está en estado "listo_para_despacho"?**  
  El Despachador Logístico es responsable de verificar el estado físico de los paquetes antes de confirmar. El sistema no bloquea automáticamente la confirmación por este motivo, pero el Despachador puede excluir Paradas con novedades antes de confirmar.

- **¿Puede crearse una Parada en una ruta que ya está en estado "lista_para_despacho" o superior?**  
  No. Una vez que la ruta pasa de "creada" a cualquier estado posterior, no acepta nuevas Paradas. El paquete que llegue a esa zona deberá iniciar una nueva ruta para la misma zona.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-001** | El sistema DEBE recibir la solicitud de ruta del Sistema de Gestión de Paquetes cuando un paquete alcanza el estado "recibido_en_sede", incluyendo `paquete_id`, coordenadas GPS de destino, dirección, peso en kg y `fecha_limite_entrega`. |
| **FR-002** | El sistema DEBE agrupar paquetes en rutas según proximidad geográfica de sus coordenadas GPS de destino, usando el parámetro configurable `radio_zona_km`. |
| **FR-003** | El sistema DEBE crear automáticamente una Parada en estado "pendiente" dentro de la ruta correspondiente cada vez que un paquete es asignado, tanto en rutas existentes como en rutas recién creadas. La Parada debe registrar: `paquete_id`, dirección, coordenadas GPS (`latitud`, `longitud`) y `fecha_limite_entrega`. |
| **FR-004** | El sistema DEBE asignar el tipo de vehículo más pequeño disponible que soporte el peso acumulado de la ruta al momento de su creación, comenzando siempre por Moto. |
| **FR-005** | El sistema DEBE reasignar automáticamente al siguiente tipo de vehículo cuando el peso acumulado supere el 90% de la capacidad del tipo actual, siguiendo el orden: Moto (20 kg) → Van (500 kg) → NHR (2.000 kg) → Turbo (4.500 kg). |
| **FR-006** | El sistema DEBE calcular la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días` al crear una ruta nueva. |
| **FR-007** | El sistema DEBE transicionar automáticamente una ruta a estado "lista_para_despacho" cuando se alcance su `fecha_limite_despacho`, independientemente del número de Paradas acumuladas, y notificar al Despachador Logístico indicando que la transición fue por vencimiento de plazo. |
| **FR-008** | El sistema DEBE transicionar una ruta a estado "lista_para_despacho" y notificar al Despachador Logístico cuando el peso acumulado alcance el 90% de la capacidad del Turbo, al no existir un tipo de vehículo de mayor capacidad. |
| **FR-009** | El sistema DEBE recibir y almacenar la `fecha_limite_entrega` de cada paquete al crear su Parada, para monitorear vencimientos y notificar al Despachador Logístico. |
| **FR-010** | El sistema NO DEBE permitir la creación de nuevas Paradas en rutas cuyo estado sea diferente a "creada". |

---

## Key Entities

| Entidad | Atributos relevantes a esta historia |
|---|---|
| **Ruta** | `ruta_id`, `zona_geografica`, `estado` ("creada" / "lista_para_despacho" / "confirmada" / "en_transito" / "cerrada"), `peso_acumulado_kg`, `tipo_vehiculo_requerido`, `fecha_creacion_ruta`, `fecha_limite_despacho`, `paradas` (lista de Paradas). |
| **Parada** | `paquete_id` (referencia al Sistema de Gestión de Paquetes), `direccion`, `latitud`, `longitud`, `fecha_limite_entrega`, `estado` ("pendiente" al momento de creación). Los demás atributos operativos de la Parada (`foto_evidencia_url`, `firma_receptor`, `motivo_novedad`, etc.) son gestionados en MOD2-UC-007. |
| **Tipo de Vehículo** | `tipo` (Moto / Van / NHR / Turbo), `capacidad_maxima_kg`. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | El sistema asigna o genera una ruta para cada paquete en estado "recibido_en_sede", crea la Parada correspondiente en estado "pendiente" y retorna el `ruta_id` al Sistema de Gestión de Paquetes sin intervención manual. |
| **SC-002** | El sistema reasigna el tipo de vehículo correctamente cada vez que el peso acumulado supera el 90% de la capacidad del tipo actual. |
| **SC-003** | El sistema transiciona la ruta a "lista_para_despacho" y notifica al Despachador Logístico en menos de 5 segundos cuando el peso acumulado alcanza el 90% del Turbo. |
| **SC-004** | El sistema transiciona automáticamente a "lista_para_despacho" toda ruta que alcance su `fecha_limite_despacho` sin importar el número de Paradas acumuladas, y notifica al Despachador Logístico en menos de 5 segundos. |
| **SC-005** | El Despachador Logístico puede transicionar manualmente cualquier ruta en estado "creada" a "lista_para_despacho" sin restricciones de capacidad ni tiempo. |
| **SC-006** | Ninguna Parada es creada en una ruta cuyo estado sea diferente a "creada". Los paquetes que lleguen a una zona con ruta en estado posterior generan automáticamente una nueva ruta para esa zona. |