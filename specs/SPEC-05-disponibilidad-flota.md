# Feature Specification: Consulta de Disponibilidad de la Flota

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 5 – Consultar Disponibilidad de la Flota (Priority: P2)

**Como** Administrador de Flota,  
**quiero** consultar en tiempo real el estado y disponibilidad de todos los vehículos,  
**para** tomar decisiones operativas sobre asignación y planificación de rutas.

**Why this priority:** Permite al Administrador tener visibilidad de la operación, pero no bloquea el flujo principal si no está disponible de forma inmediata.

**Independent Test:** Se puede probar consultando el panel de flota y verificando que refleja correctamente los estados actuales de cada vehículo.

**Acceptance Scenarios:**

1. **Scenario:** Consulta exitosa
   - **Given:** El Administrador tiene sesión activa y existen vehículos registrados en el sistema.
   - **When:** Consulta el panel de disponibilidad de la flota.
   - **Then:** El sistema muestra el estado actualizado de cada vehículo con su conductor asignado, zona de operación y estado operativo.

2. **Scenario:** Consulta sin vehículos disponibles
   - **Given:** Todos los vehículos registrados se encuentran en un estado no disponible (en tránsito, inactivo o en mantenimiento).
   - **When:** El Administrador consulta el panel de disponibilidad.
   - **Then:** El sistema informa que no hay vehículos disponibles y muestra el detalle del estado actual de cada uno.

---

## Edge Cases

- ¿Con qué frecuencia se actualiza el panel de disponibilidad de la flota?  
  El panel se actualiza en tiempo real cada vez que cambia el estado de un vehículo. No hay un intervalo fijo de sincronización; el cambio de estado es el disparador de la actualización.

- ¿Cómo se muestra un vehículo disponible pero sin conductor asignado?  
  El panel lo diferencia visualmente de un vehículo disponible con conductor, dado que no puede ser tomado por el algoritmo de planificación aunque su estado técnico sea 'Disponible'.

---

## Functional Requirements

Este caso de uso no genera nuevos requerimientos funcionales propios. La visibilidad del panel depende del correcto funcionamiento de FR-006, FR-008 y FR-009 definidos en las specs de gestión de vehículos y asignación de conductores.

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-013** | El panel refleja el estado actual de cada vehículo en tiempo real, actualizándose con cada cambio de estado sin necesidad de recargar la página. |
| **SC-014** | Los vehículos disponibles sin conductor asignado se distinguen visualmente de los disponibles con conductor, para evitar confusión al planificar. |
