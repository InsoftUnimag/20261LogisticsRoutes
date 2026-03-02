# Feature Specification: Planificación Automática de Rutas

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 1 – Planificar Rutas (Priority: P1)

**Como** Sistema de Gestión de Paquetes (Módulo 1),  
**quiero** solicitar una ruta cuando un paquete alcanza el estado 'Listo para Despacho',  
**para que** el sistema ejecute el algoritmo de consolidación de carga y planifique la ruta óptima sin intervención manual.

**Why this priority:** Es la integración crítica entre sistemas y el disparador de todo el flujo logístico. Sin ella, los paquetes quedan en estado 'Listo para Despacho' indefinidamente.

**Independent Test:** Se puede probar registrando un paquete en estado 'Listo para Despacho' y verificando que el sistema genera o asigna automáticamente una ruta con vehículo y conductor asignados.

**Acceptance Scenarios:**

1. **Scenario:** Asignación a ruta existente
   - **Given:** Un paquete se encuentra en estado 'Listo para Despacho' y existe una ruta activa cuya zona geográfica coincide con su destino y tiene capacidad disponible.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema asigna el paquete a la ruta existente y actualiza su estado a 'En Ruta'.

2. **Scenario:** Nueva ruta por zona diferente
   - **Given:** Un paquete se encuentra en estado 'Listo para Despacho' y no existe una ruta activa que coincida con su zona geográfica de destino.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema genera una nueva ruta, asigna el vehículo y conductor óptimos disponibles en esa zona y notifica al Despachador Logístico.

3. **Scenario:** Nueva ruta por capacidad agotada
   - **Given:** Un paquete se encuentra en estado 'Listo para Despacho' y la ruta existente para su zona ha alcanzado el 90% de la capacidad del vehículo asignado.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema genera una nueva ruta para esa misma zona, asigna el paquete y notifica al Despachador Logístico.

4. **Scenario:** Planificación bloqueada sin recursos
   - **Given:** Un paquete se encuentra en estado 'Listo para Despacho' pero no hay vehículo disponible con conductor activo en la zona correspondiente.
   - **When:** El sistema evalúa la asignación del paquete.
   - **Then:** El sistema no genera la ruta, notifica al Despachador Logístico indicando la causa y el paquete permanece en estado 'Listo para Despacho'.

---

## Edge Cases

- ¿Qué ocurre cuando el evento `solicitar_ruta` llega con campos obligatorios faltantes (peso, coordenadas, tipo de mercancía, etc.)?  
  El sistema rechaza la solicitud y responde al Sistema de Gestión de Paquetes (Módulo 1) con el detalle del campo faltante. No se genera ningún registro de ruta.

- ¿Cómo maneja el sistema un paquete cuya `fecha_limite_entrega` está próxima a vencer (1 día o menos)?  
  El sistema lo asigna con prioridad alta dentro de la ruta correspondiente y notifica al Despachador Logístico para despacho urgente.

- ¿Qué ocurre cuando la `fecha_limite_entrega` del paquete ya venció al momento de recibir el evento `solicitar_ruta`?  
  El sistema rechaza la solicitud y notifica al Sistema de Gestión de Paquetes (Módulo 1) para que gestione la situación con el cliente.

- ¿Cómo maneja el sistema dos solicitudes `solicitar_ruta` simultáneas para la misma zona cuando la ruta existente solo tiene capacidad para una?  
  El sistema atiende la primera en llegar y para la segunda crea una nueva ruta. No puede haber doble asignación al mismo slot de capacidad.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-001** | El sistema DEBE recibir el evento `solicitar_ruta` del Sistema de Gestión de Paquetes (Módulo 1) y ejecutar el algoritmo de consolidación de carga para asignar el paquete a una ruta existente o crear una nueva. |
| **FR-002** | El sistema DEBE asignar el paquete a una ruta existente si la zona geográfica coincide y tiene capacidad disponible. |
| **FR-003** | El sistema DEBE generar una nueva ruta cuando no existe una ruta compatible o la existente ha alcanzado el 90% de su capacidad. |
| **FR-004** | El sistema DEBE seleccionar el vehículo de menor capacidad disponible que soporte el peso del conjunto de paquetes asignados. |
| **FR-016** | El sistema DEBE notificar al Despachador Logístico ante cualquier bloqueo en la planificación de rutas. |
| **FR-022** | El sistema DEBE recibir y almacenar la `fecha_limite_entrega` de cada paquete para priorizar la planificación y emitir alertas de vencimiento. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | El sistema asigna o genera una ruta para un paquete en estado 'Listo para Despacho' sin intervención manual. |
| **SC-002** | El algoritmo selecciona el vehículo de menor capacidad disponible que cumpla el requerimiento de peso total de los paquetes asignados. |
| **SC-003** | El sistema responde al Sistema de Gestión de Paquetes (Módulo 1) con la confirmación de asignación (incluyendo `ruta_id`) en cada solicitud procesada correctamente. |
