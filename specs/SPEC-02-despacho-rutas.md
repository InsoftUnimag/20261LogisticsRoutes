# Feature Specification: Despacho de Rutas

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 2 – Despachar Paquetes a Rutas (Priority: P1)

**Como** Despachador Logístico,  
**quiero** confirmar que los paquetes están cargados en el vehículo y despachar los paquetes asignados a la ruta,  
**para que** los paquetes avancen al estado 'En Tránsito', el conductor reciba la ruta en su dispositivo y el Sistema de Gestión de Paquetes (Módulo 1) sea notificado del cambio de estado.

**Why this priority:** Es el punto de activación de la operación de entrega y el evento que notifica al Sistema de Gestión de Paquetes (Módulo 1) que los paquetes están en camino. Sin el despacho, ninguna ruta planificada se ejecuta.

**Independent Test:** Se puede probar tomando una ruta planificada, ejecutando el despacho y verificando que el conductor recibe la ruta en su dispositivo, los paquetes cambian a 'En Tránsito' y el Sistema de Gestión de Paquetes (Módulo 1) recibe la notificación.

**Acceptance Scenarios:**

1. **Scenario:** Despacho exitoso
   - **Given:** Una ruta tiene paquetes asignados, vehículo con capacidad adecuada y conductor operativo disponible.
   - **When:** El Despachador Logístico confirma el despacho.
   - **Then:** La ruta y los paquetes pasan a estado 'En Tránsito', el conductor recibe la ruta en su dispositivo y el Sistema de Gestión de Paquetes (Módulo 1) es notificado para actualizar el estado de cada paquete a 'En Tránsito'.

2. **Scenario:** Despacho bloqueado — conductor no disponible
   - **Given:** Una ruta tiene vehículo disponible pero el conductor asignado dejó de estar operativo.
   - **When:** El Despachador intenta confirmar el despacho.
   - **Then:** El sistema bloquea el despacho, informa la causa y sugiere reasignar un conductor habilitado. La ruta permanece en estado 'Listo para Despacho'.

3. **Scenario:** Despacho bloqueado — sobrepeso
   - **Given:** Un paquete agregado a la ruta lleva el peso total por encima de la capacidad permitida del vehículo asignado.
   - **When:** El Despachador intenta confirmar el despacho.
   - **Then:** El sistema bloquea el despacho, informa la causa y sugiere ajustar los paquetes o reasignar a un vehículo de mayor capacidad.

4. **Scenario:** Despacho con exclusión de paquete por novedad
   - **Given:** Un paquete asignado a la ruta presenta una novedad física identificada antes del despacho.
   - **When:** El Despachador confirma el despacho.
   - **Then:** El paquete con novedad queda excluido de la ruta, cambia al estado correspondiente y se registra el motivo. La ruta se despacha con los paquetes restantes y el Sistema de Gestión de Paquetes (Módulo 1) es notificado del cambio de estado de cada paquete.

---

## Edge Cases

- ¿Qué ocurre cuando la ruta está próxima a vencer la `fecha_limite_asignacion` de alguno de sus paquetes y aún no ha sido despachada?  
  El sistema emite una alerta de alta prioridad al Despachador Logístico indicando cuáles paquetes están en riesgo de vencer su `fecha_limite_entrega`.

- ¿Qué ocurre cuando el vehículo asignado es dado de baja entre la planificación y el momento del despacho?  
  El sistema bloquea el despacho e informa al Despachador que el vehículo ya no está disponible, solicitando reasignación antes de continuar.

- ¿Puede el Despachador modificar el orden de paradas de una ruta después de confirmar el despacho?  
  No. Los ajustes al orden de paradas solo pueden realizarse antes de confirmar el despacho. Una vez confirmado, el orden queda fijo.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-005** | El sistema DEBE bloquear el despacho si el peso total de los paquetes supera el umbral permitido del vehículo asignado. |
| **FR-016** | El sistema DEBE notificar al Despachador Logístico ante cualquier bloqueo en el despacho de rutas. |
| **FR-021** | El sistema DEBE registrar la `fecha_hora_inicio` de la ruta en el momento exacto en que el Despachador confirma el despacho. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-004** | El Conductor recibe la ruta en su dispositivo inmediatamente después de que el Despachador confirma el despacho. |
| **SC-005** | El Sistema de Gestión de Paquetes (Módulo 1) recibe la notificación de cambio de estado a 'En Tránsito' para cada paquete de la ruta despachada. |
| **SC-006** | La `fecha_hora_inicio` queda registrada en el momento exacto del despacho, sin margen de error. |
