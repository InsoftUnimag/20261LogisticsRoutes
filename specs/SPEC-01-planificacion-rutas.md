# Feature Specification: Planificación Automática de Rutas (MOD2-UC-001)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 1 – Planificar Rutas (Priority: P1)

**Como** Sistema de Gestión de Paquetes (Módulo 1),  
**quiero** solicitar una ruta cuando un paquete alcanza el estado 'Listo para Despacho',  
**para que** el sistema agrupe el paquete con otros de la misma zona geográfica, gestione el tipo de vehículo requerido y lo tenga listo para despacho dentro del plazo establecido.

**Why this priority:** Es la integración crítica entre sistemas y el disparador de todo el flujo logístico. Sin ella, los paquetes quedan en estado 'Listo para Despacho' indefinidamente sin avanzar.

**Independent Test:** Se puede probar enviando la solicitud de ruta para un paquete en estado 'Listo para Despacho' y verificando que el sistema retorna un `ruta_id` válido, agrupa el paquete en la ruta correcta según sus coordenadas GPS y actualiza el tipo de vehículo requerido si el peso acumulado lo exige. El paquete debe permanecer en estado 'Listo para Despacho' en el Módulo 1.

---

## Acceptance Scenarios

1. **Scenario:** Asignación a ruta existente con capacidad disponible
   - **Given:** Un paquete en estado 'Listo para Despacho' llega al sistema con sus coordenadas GPS y existe una ruta en estado 'En Espera' cuya zona geográfica coincide y el peso acumulado no ha superado el 90% de la capacidad del tipo de vehículo requerido.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema agrega el paquete a la ruta existente, actualiza el peso acumulado, recalcula el porcentaje de capacidad utilizada y retorna al Módulo 1 el `ruta_id`. El paquete permanece en estado 'Listo para Despacho' en el Módulo 1.

2. **Scenario:** Creación de nueva ruta por zona sin ruta activa
   - **Given:** Un paquete en estado 'Listo para Despacho' llega al sistema y no existe ninguna ruta en estado 'En Espera' cuya zona geográfica coincida con su destino.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema crea una nueva ruta para esa zona en estado 'En Espera', asigna el tipo de vehículo más pequeño que soporte el peso inicial (comenzando por Moto), registra la `fecha_creacion_ruta` y calcula la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días`. Retorna al Módulo 1 el `ruta_id`. El paquete permanece en estado 'Listo para Despacho' en el Módulo 1.

3. **Scenario:** Reasignación de tipo de vehículo por peso acumulado
   - **Given:** Un paquete llega a una ruta en estado 'En Espera' y al sumar su peso el total supera la capacidad del tipo de vehículo actualmente requerido por la ruta.
   - **When:** El sistema recalcula el peso acumulado tras agregar el paquete.
   - **Then:** El sistema reasigna automáticamente al siguiente tipo de vehículo disponible en el orden establecido (Moto 20kg → Van 500kg → NHR 2,000kg → Turbo 4,500kg), agrega el paquete a la ruta y retorna al Módulo 1 el `ruta_id`. El paquete permanece en estado 'Listo para Despacho' en el Módulo 1.

4. **Scenario:** Alerta de ruta lista por capacidad al 90%
   - **Given:** Un paquete es agregado a una ruta y el peso acumulado resultante alcanza o supera el 90% de la capacidad del tipo de vehículo requerido.
   - **When:** El sistema recalcula el porcentaje de capacidad tras agregar el paquete.
   - **Then:** El sistema notifica al Despachador Logístico que la ruta está lista para despachar, indicando la zona, número de paquetes, peso acumulado y tipo de vehículo requerido. La ruta pasa a estado 'Lista para Despacho'.

5. **Scenario:** Planificación bloqueada por tipo de vehículo no disponible
   - **Given:** Un paquete llega al sistema y el tipo de vehículo requerido para soportar el peso acumulado de su zona no tiene unidades disponibles en ese momento.
   - **When:** El sistema intenta asignar el tipo de vehículo correspondiente.
   - **Then:** El sistema notifica al Despachador Logístico indicando la zona afectada, el peso acumulado y el tipo de vehículo requerido que no está disponible. El Módulo 1 es informado de que la ruta no pudo ser confirmada y el paquete permanece en estado 'Listo para Despacho'.

---

## Edge Cases

- **¿Qué ocurre cuando la solicitud llega con campos obligatorios faltantes (peso, coordenadas, fecha_limite_entrega, etc.)?**  
  El sistema rechaza la solicitud y responde al Módulo 1 con el detalle del campo faltante. No se genera ningún registro de ruta.

- **¿Qué ocurre cuando la `fecha_limite_entrega` del paquete ya venció al momento de recibir la solicitud?**  
  El sistema rechaza la solicitud y notifica al Módulo 1 para que gestione la situación con el cliente.

- **¿Qué pasa si el peso de un paquete por sí solo supera la capacidad del Camión Turbo (4,500kg)?**  
  El sistema no puede asignar el paquete a ninguna ruta. Notifica al Despachador Logístico y al Módulo 1 como un caso especial que requiere gestión manual.

- **¿Qué pasa si dos paquetes de la misma zona llegan simultáneamente y ambos intentan crear una ruta nueva al mismo tiempo?**  
  El sistema debe garantizar que solo se crea una ruta por zona en ese instante mediante control de concurrencia a nivel de base de datos. No puede haber rutas duplicadas por zona.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-001** | El sistema DEBE recibir la solicitud de ruta del Módulo 1 cuando un paquete alcanza el estado 'Listo para Despacho', incluyendo `paquete_id`, coordenadas GPS de destino, peso en kg y `fecha_limite_entrega`. |
| **FR-002** | El sistema DEBE agrupar paquetes en rutas según proximidad geográfica de sus coordenadas GPS de destino. |
| **FR-003** | El sistema DEBE asignar el tipo de vehículo más pequeño disponible que soporte el peso acumulado de la ruta al momento de su creación, comenzando siempre por Moto. |
| **FR-004** | El sistema DEBE reasignar automáticamente al siguiente tipo de vehículo cuando el peso acumulado supere la capacidad del tipo actual, siguiendo el orden: Moto (20kg) → Van (500kg) → NHR (2,000kg) → Turbo (4,500kg). |
| **FR-005** | El sistema DEBE calcular la `fecha_limite_despacho` como `fecha_creacion_ruta + 5 días` al crear una ruta nueva. |
| **FR-006** | El sistema DEBE notificar al Despachador Logístico cuando una ruta alcance el 90% de la capacidad del tipo de vehículo requerido, cambiando su estado a 'Lista para Despacho'. |
| **FR-007** | El sistema DEBE notificar al Despachador Logístico cuando no haya unidades disponibles del tipo de vehículo requerido para una zona. |
| **FR-008** | El sistema NO DEBE cambiar el estado del paquete en el Módulo 1 durante la planificación. El paquete permanece en 'Listo para Despacho' hasta que el Conductor confirme el inicio del tránsito. |
| **FR-022** | El sistema DEBE recibir y almacenar la `fecha_limite_entrega` de cada paquete para monitorear vencimientos y emitir alertas al Despachador Logístico. |

---

## Key Entities

| Entidad | Atributos relevantes a esta historia |
|---|---|
| **Ruta** | `ruta_id`, `zona_geografica`, `estado` (En Espera / Lista para Despacho), `peso_acumulado_kg`, `tipo_vehiculo_requerido`, `fecha_creacion_ruta`, `fecha_limite_despacho`, `lista_paquetes`. |
| **Tipo de Vehículo** | `tipo` (Moto / Van / NHR / Turbo), `capacidad_maxima_kg`, `unidades_disponibles`. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | El sistema asigna o genera una ruta para cada paquete en estado 'Listo para Despacho' y retorna el `ruta_id` al Módulo 1 sin intervención manual. |
| **SC-002** | El sistema reasigna el tipo de vehículo correctamente cada vez que el peso acumulado supera la capacidad del tipo actual. |
| **SC-003** | El sistema notifica al Despachador Logístico en menos de 5 segundos cuando una ruta alcanza el 90% de capacidad. |
