# Feature Specification: Consulta de Ruta por el Conductor (MOD2-UC-006)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 6 – Consultar Ruta Asignada (Priority: P2)

**Como** Conductor,  
**quiero** consultar mi ruta asignada e iniciar el tránsito una vez verificada la carga,  
**para** conocer el orden de paradas, las direcciones y el detalle de cada paquete, y confirmar que ya voy en camino a entregar.

**Why this priority:** Sin acceso a la ruta el conductor no puede ejecutar ninguna entrega. Es el punto de entrada a toda la operación de campo y el momento en que la ruta pasa oficialmente a tránsito.

**Independent Test:** Se puede probar despachando una ruta hacia un conductor, verificando que la consulta muestra todas las paradas correctamente y que al confirmar el inicio la ruta pasa a estado 'En Tránsito' con la fecha de salida registrada.

---

## Acceptance Scenarios

1. **Scenario:** Consulta exitosa de ruta
   - **Given:** El Conductor tiene sesión activa y una ruta ha sido confirmada por el Despachador y está en estado "confirmada".
   - **When:** Consulta su ruta activa.
   - **Then:** El sistema muestra la ruta con el orden de paradas, dirección, y detalle de cada paquete.

2. **Scenario:** Inicio de tránsito por el conductor
   - **Given:** El Conductor revisó su ruta, verificó que los paquetes están cargados en el vehículo y está listo para salir.
   - **When:** Confirma el inicio del tránsito en su dispositivo.
   - **Then:** La ruta pasa a estado "en_transito", se registra la fecha y hora de salida. El Sistema de Gestión de Paquetes recibe el evento "paquete_en_tránsito" para que cada paquete de la ruta cambie a estado "en_transito".

3. **Scenario:** Consulta sin ruta asignada
   - **Given:** El Conductor tiene sesión activa pero no tiene ninguna ruta en estado "confirmada" asignada a su dispositivo.
   - **When:** Consulta su ruta activa.
   - **Then:** El sistema informa que no hay rutas asignadas disponibles para el conductor en ese momento.

---

## Edge Cases

- **¿Qué ocurre cuando el conductor pierde conexión a internet durante la jornada?**  
  La ruta debe estar disponible en modo offline desde el momento del despacho. Los registros de paradas realizados sin conexión se sincronizan al recuperar conectividad, y el timestamp de cada registro corresponde al momento real en que el conductor ejecutó la acción, no al de la sincronización.

- **¿Qué ocurre si el conductor confirma el inicio del tránsito pero en realidad no cargó todos los paquetes?**  
  El sistema no puede validar la carga física. La responsabilidad de verificar la carga antes de confirmar es del conductor. Si un paquete no fue cargado, deberá registrarse como novedad en la parada correspondiente.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-021** | El sistema DEBE mostrar al Conductor su ruta asignada en estado "confirmada" con el orden de paradas, dirección y detalle de cada paquete. |
| **FR-023** | El sistema DEBE permitir al Conductor confirmar el inicio del tránsito desde su dispositivo una vez verificada la carga. |
| **FR-024** | Al confirmar el inicio del tránsito, el sistema DEBE cambiar el estado de la ruta a "en_transito", registrar la fecha y hora de salida y recibir el evento "paquete_en_tránsito" del Sistema de Gestión de Paquetes para que cada paquete pase a estado "en_transito". |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-015** | El conductor puede consultar su ruta completa en su dispositivo inmediatamente tras la confirmación del despacho por parte del Despachador Logístico. |
| **SC-016** | La ruta permanece accesible en modo offline si el conductor pierde conectividad durante la jornada, sin pérdida de información de paradas. |
| **SC-021** | Al confirmar el inicio del tránsito, el Sistema de Gestión de Paquetes recibe el evento "paquete_en_tránsito" y actualiza el estado de todos los paquetes de la ruta a "en_transito" en menos de 5 segundos. |
