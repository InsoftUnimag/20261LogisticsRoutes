# Feature Specification: Consulta de Ruta por el Conductor

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 6 – Consultar Ruta Asignada (Priority: P2)

**Como** Conductor,  
**quiero** consultar mi ruta asignada,  
**para** conocer el orden de paradas, las direcciones y el detalle de cada paquete antes de iniciar la operación.

**Why this priority:** Sin acceso a la ruta, el conductor no puede ejecutar ninguna entrega. Es el punto de entrada a toda la operación de campo.

**Independent Test:** Se puede probar despachando una ruta hacia un conductor y verificando que el sistema la muestra correctamente en su dispositivo con todas las paradas y detalles.

**Acceptance Scenarios:**

1. **Scenario:** Consulta exitosa de ruta
   - **Given:** El Conductor tiene sesión activa y una ruta ha sido despachada hacia su dispositivo.
   - **When:** Consulta su ruta activa.
   - **Then:** El sistema muestra la ruta con el orden de paradas, dirección, fecha/hora estimada y detalle de cada paquete.

2. **Scenario:** Consulta sin ruta asignada
   - **Given:** El Conductor tiene sesión activa pero no tiene ninguna ruta despachada hacia su dispositivo.
   - **When:** Consulta su ruta activa.
   - **Then:** El sistema informa que no hay rutas asignadas disponibles para el conductor en ese momento.

---

## Edge Cases

- ¿Qué ocurre cuando el conductor pierde conexión a internet durante la jornada?  
  La ruta debe estar disponible en modo offline desde el momento del despacho. Los registros de paradas realizados sin conexión se sincronizan al recuperar conectividad, y el timestamp de cada registro corresponde al momento real en que el conductor ejecutó la acción, no al de la sincronización.

---

## Functional Requirements

Este caso de uso no genera requerimientos funcionales nuevos independientes. Depende del correcto funcionamiento de FR-021 (registro de `fecha_hora_inicio`) y del mecanismo de envío de ruta al dispositivo del conductor establecido en SPEC-02.

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-015** | El conductor puede consultar su ruta completa en su dispositivo inmediatamente tras la confirmación del despacho por parte del Despachador Logístico. |
| **SC-016** | La ruta permanece accesible en modo offline si el conductor pierde conectividad durante la jornada, sin pérdida de información de paradas. |
