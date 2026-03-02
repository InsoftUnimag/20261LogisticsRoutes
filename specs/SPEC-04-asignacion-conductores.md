# Feature Specification: Asignación de Conductores a Vehículos

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 4 – Asignar Conductor a Vehículo (Priority: P2)

**Como** Administrador de Flota,  
**quiero** asignar y gestionar conductores a los vehículos de la flota,  
**para** garantizar que cada vehículo operativo cuente con un conductor habilitado disponible.

**Why this priority:** Sin conductores asignados correctamente, ninguna ruta puede despacharse. Depende de la gestión de vehículos pero es independiente del flujo de paquetes.

**Independent Test:** Se puede probar asignando un conductor a un vehículo disponible y verificando que el sistema lo refleja correctamente al momento de planificar una ruta.

**Acceptance Scenarios:**

1. **Scenario:** Asignación exitosa
   - **Given:** Existe un vehículo en estado 'Disponible' y un conductor activo sin vehículo asignado.
   - **When:** El Administrador asigna el conductor al vehículo.
   - **Then:** El sistema registra la asignación y el conductor queda vinculado al vehículo con estado operativo activo.

2. **Scenario:** Asignación bloqueada — conductor ya asignado
   - **Given:** Un conductor ya se encuentra asignado a otro vehículo activo.
   - **When:** El Administrador intenta asignarlo a un segundo vehículo.
   - **Then:** El sistema rechaza la asignación e informa que el conductor ya tiene un vehículo activo asignado.

3. **Scenario:** Reasignación bloqueada — vehículo en tránsito
   - **Given:** Un vehículo se encuentra en estado 'En Tránsito' con una ruta activa.
   - **When:** El Administrador intenta cambiar el conductor asignado.
   - **Then:** El sistema rechaza la reasignación e informa que el vehículo tiene una ruta activa en curso.

---

## Edge Cases

- ¿Qué ocurre cuando un conductor es dado de baja del sistema mientras tiene una ruta activa en tránsito?  
  El sistema emite una alerta de alta prioridad al Despachador Logístico para gestión operativa inmediata. La ruta no se cancela automáticamente — el Despachador decide cómo proceder según la situación en campo.

- ¿Qué ocurre cuando el Administrador desvincula un conductor de un vehículo disponible (sin ruta activa)?  
  El sistema registra la fecha y hora de fin de la asignación en el historial y deja el vehículo sin conductor asignado. El vehículo queda excluido del algoritmo de planificación hasta que se le asigne un nuevo conductor.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-009** | El sistema DEBE permitir al Administrador de Flota asignar un conductor activo a un vehículo en estado 'Disponible'. |
| **FR-010** | El sistema DEBE impedir que un conductor sea asignado a más de un vehículo activo simultáneamente. |
| **FR-011** | El sistema DEBE impedir la reasignación de conductor en vehículos con rutas activas. |
| **FR-012** | El sistema DEBE registrar el historial de asignaciones conductor-vehículo con fecha y hora de inicio y fin. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-010** | El sistema impide en todo momento que un conductor esté asignado a más de un vehículo activo simultáneamente, sin excepción. |
| **SC-011** | El historial de asignaciones queda disponible con fecha y hora de inicio y fin para consulta y auditoría por parte del Administrador. |
| **SC-012** | Un vehículo sin conductor asignado queda excluido del algoritmo de planificación de forma inmediata tras la desvinculación. |
