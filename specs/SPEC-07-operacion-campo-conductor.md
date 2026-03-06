# Feature Specification: Operación de Campo del Conductor (MOD2-UC-007)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 7a – Registrar Resultado de Parada (Priority: P2)

**Como** Conductor,  
**quiero** registrar el resultado de cada parada, ya sea una entrega exitosa, una parada fallida o una novedad grave,  
**para que** el Sistema de Gestión de Paquetes (Módulo 1) se mantenga actualizado con el estado de cada paquete en tiempo real y los datos queden registrados para el cierre de ruta.

**Why this priority:** Es el núcleo operativo del módulo. El conductor genera los datos de cada parada; el sistema los clasifica, notifica al Módulo 1 en tiempo real y los acumula para el cierre de ruta.

**Independent Test:** Se puede probar registrando una entrega exitosa y una parada fallida en una ruta activa, y verificando que el Módulo 1 actualiza el estado de cada paquete correctamente.

---

## Acceptance Scenarios

1. **Scenario:** Registro de entrega exitosa
   - **Given:** El Conductor se encuentra en la parada de entrega con el paquete correspondiente.
   - **When:** Registra la entrega con foto y firma como evidencia (POD).
   - **Then:** La parada cambia a estado 'Exitosa', el Módulo 1 es notificado para actualizar el paquete a 'Entregado' y el dato queda registrado para el cierre de ruta.

2. **Scenario:** Registro de parada fallida
   - **Given:** El Conductor se encuentra en la parada pero no puede completar la entrega.
   - **When:** Registra la parada como fallida con el motivo de no entrega correspondiente (cliente ausente, dirección incorrecta, rechazado por cliente o zona de difícil acceso).
   - **Then:** La parada cambia a estado 'Fallida' con su motivo registrado, el Módulo 1 es notificado para que actualice el estado del paquete y tome la decisión correspondiente, y el dato queda registrado para el cierre de ruta.

3. **Scenario:** Registro de novedad grave en ruta
   - **Given:** El Conductor detecta durante la ruta que un paquete está dañado, fue extraviado o requiere devolución.
   - **When:** Registra la novedad grave con el tipo de novedad correspondiente.
   - **Then:** El Módulo 1 es notificado de forma inmediata para que active el protocolo correspondiente (seguro o devolución al origen) y el evento queda registrado para el cierre de ruta.

---

## User Story 7b – Cerrar Ruta (Priority: P1)

**Como** Conductor,  
**quiero** cerrar la ruta una vez finalizadas todas las paradas,  
**para que** el sistema genere el informe de cierre consolidado y lo envíe al Sistema de Facturación y Liquidación (Módulo 3) para que calcule la liquidación correspondiente.

**Why this priority:** Es el paso final del flujo operativo. Sin el cierre, el Módulo 3 no puede calcular la liquidación del conductor.

**Independent Test:** Se puede probar completando todas las paradas de una ruta, ejecutando el cierre y verificando que el Módulo 3 recibe el evento `ruta_cerrada` con la estructura correcta.

---

## Acceptance Scenarios

1. **Scenario:** Cierre exitoso con todas las paradas gestionadas
   - **Given:** El Conductor ha gestionado todas las paradas de la ruta con su respectivo estado (exitosa, fallida o novedad).
   - **When:** Solicita el cierre de la ruta.
   - **Then:** La ruta cambia a estado 'Cerrada', el sistema genera y envía al Módulo 3 el evento `ruta_cerrada` con el detalle completo de todas las paradas.

2. **Scenario:** Intento de cierre con paradas pendientes
   - **Given:** El Conductor intenta cerrar la ruta pero existen paradas sin gestionar.
   - **When:** Solicita el cierre de la ruta.
   - **Then:** El sistema informa cuáles paradas están pendientes, advierte que si no las gestiona el sistema cerrará la ruta automáticamente al vencer el tiempo límite de 2 días en tránsito, y le da la opción de gestionar las pendientes o confirmar el cierre manual asumiendo que el sistema marcará las restantes como novedad automática.

3. **Scenario:** Cierre automático por tiempo excedido
   - **Given:** La ruta lleva más de 2 días en estado 'En Tránsito' sin que el conductor haya ejecutado el cierre manual y existen paradas sin gestionar.
   - **When:** El sistema detecta que se superaron los 2 días en tránsito.
   - **Then:** El sistema marca todas las paradas sin gestionar con estado `sin_gestión_conductor` y origen `sistema`, cambia el estado de la ruta a 'Cerrada (Automático)', genera y envía el evento `ruta_cerrada` al Módulo 3 con `tipo_cierre: automático`, notifica al Módulo 1 el estado final de cada paquete y envía alerta de alta prioridad al Despachador Logístico.

---

## Edge Cases

- **¿Qué ocurre cuando el conductor intenta registrar una entrega exitosa sin adjuntar foto de evidencia (POD)?**  
  El sistema rechaza el registro e indica que la foto POD es obligatoria para confirmar una entrega exitosa. El conductor debe adjuntarla antes de continuar.

- **¿Puede el Despachador Logístico forzar el cierre de una ruta si el conductor no lo hace dentro del tiempo establecido?**  
  Sí. El Despachador puede forzar el cierre desde su panel. El cierre forzado se registra con `tipo_cierre: forzado_despachador` y aplica la misma lógica que el cierre automático para las paradas sin gestionar.

- **¿Qué ocurre cuando el conductor cierra la ruta sin conexión a internet?**  
  El sistema almacena la acción localmente y ejecuta el cierre formal (incluyendo el envío de eventos a los sistemas externos) al recuperar conectividad. El timestamp de cierre corresponde al momento en que el conductor ejecutó la acción, no al de la sincronización.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-013** | El sistema DEBE permitir al Conductor registrar el resultado de cada parada: exitosa, fallida con motivo, o novedad grave. |
| **FR-014** | El sistema DEBE notificar al Módulo 1 en tiempo real el estado de cada parada (entrega exitosa, parada fallida con motivo, novedad grave). |
| **FR-015** | El sistema DEBE enviar el evento `ruta_cerrada` al Módulo 3 al cerrar una ruta. |
| **FR-017** | El sistema DEBE cerrar automáticamente las rutas que superen 2 días en estado 'En Tránsito' sin cierre manual del conductor. |
| **FR-018** | El sistema DEBE marcar con estado `sin_gestión_conductor` y origen `sistema` las paradas sin gestionar al momento del cierre automático o forzado. |
| **FR-019** | El sistema DEBE notificar al Módulo 1 el estado final de cada paquete al cierre de ruta, ya sea manual, automático o forzado. |
| **FR-020** | El sistema NO DEBE permitir que el conductor interactúe directamente con el Módulo 3. El envío del evento de cierre es responsabilidad exclusiva del sistema de rutas. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-017** | El Módulo 1 recibe la notificación de cambio de estado de cada paquete en tiempo real conforme el conductor registra cada parada. |
| **SC-018** | El Módulo 3 recibe el evento `ruta_cerrada` con la estructura completa al momento del cierre, ya sea manual, automático o forzado. |
| **SC-019** | Las paradas sin gestionar al momento del cierre quedan registradas con `origen: sistema`, diferenciándolas claramente de las gestionadas por el conductor para efectos de liquidación. |
| **SC-020** | El sistema no pierde ningún registro de parada cuando el conductor opera sin conexión; todos se sincronizan correctamente al recuperar conectividad. |
