# Feature Specification: Operación de Campo del Conductor (MOD2-UC-007)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 7a – Registrar Resultado de Parada (Priority: P2)

**Como** Conductor,  
**quiero** registrar el resultado de cada parada, ya sea una entrega "exitosa", una parada "fallida" o una "novedad" grave,  
**para que** el Sistema de Gestión de Paquetes se mantenga actualizado con el estado de cada paquete en tiempo real y los datos queden registrados para el cierre de ruta.

**Why this priority:** Es el núcleo operativo del módulo. El conductor genera los datos de cada parada; el sistema los clasifica, el conductor envia el evento "paquete_entregado", "paquete_fallido" o "paquete_novedad" al Sistema de Gestión de Paquetes en tiempo real y los acumula para el cierre de ruta.

**Independent Test:** Se puede probar registrando una parada "exitosa" y una parada "fallida" en una ruta activa, y verificando que el Sistema de Gestión de Paquetes actualiza el estado de cada paquete correctamente.

---

## Acceptance Scenarios

1. **Scenario:** Registro de entrega exitosa
   - **Given:** El Conductor se encuentra en la parada de entrega con el paquete correspondiente.
   - **When:** Registra la entrega con foto y firma como evidencia (POD).
   - **Then:** La parada cambia a estado "exitosa", el Sistema de Gestión de Paquetes recibe el evento "paquete_entregado" para actualizar el paquete a "entregado" y el dato queda registrado para el cierre de ruta.

2. **Scenario:** Registro de parada fallida
   - **Given:** El Conductor se encuentra en la parada pero no puede completar la entrega.
   - **When:** Registra la parada como fallida con el motivo de no entrega correspondiente (cliente ausente, dirección incorrecta, rechazado por cliente o zona de difícil acceso).
   - **Then:** La parada cambia a estado "fallida" con su motivo registrado, el Sistema de Gestión de Paquetes recibe el evento "paquete_fallido" para actualizar el estado del paquete y tome la decisión correspondiente, y el dato queda registrado para el cierre de ruta.

3. **Scenario:** Registro de novedad grave en ruta
   - **Given:** El Conductor detecta durante la ruta que un paquete está dañado, fue extraviado o requiere devolución.
   - **When:** Registra la novedad grave con el tipo de novedad correspondiente.
   - **Then:** El Sistema de Gestión de Paquetes recibe el evento "paquete_novedad" para que active el protocolo correspondiente (seguro o devolución al origen) y el evento queda registrado para el cierre de ruta.

---

## User Story 7b – Cerrar Ruta (Priority: P1)

**Como** Conductor,  
**quiero** cerrar la ruta una vez finalizadas todas las paradas,  
**para que** el sistema genere el informe de cierre consolidado y lo envíe al Sistema de Facturación y Liquidación para que calcule la liquidación correspondiente.

**Why this priority:** Es el paso final del flujo operativo. Sin el cierre, el Sistema de Facturación y Liquidación no puede calcular la liquidación del conductor.

**Independent Test:** Se puede probar completando todas las paradas de una ruta, ejecutando el cierre y verificando que el Sistema de Facturación y Liquidación recibe el evento "cierre_ruta"con la estructura correcta.

---

## Acceptance Scenarios

1. **Scenario:** Cierre exitoso con todas las paradas gestionadas
   - **Given:** El Conductor ha gestionado todas las paradas de la ruta con su respectivo estado ("exitosa", "fallida" o "novedad").
   - **When:** Solicita el cierre de la ruta.
   - **Then:** La ruta cambia a estado "cerrada", el sistema genera y envía al Sistema de Facturación y Liquidación el evento "cierre_ruta" con el detalle completo de todas las paradas. El estado del vehículo pasa a "disponible" y el estado del conductor a "activo".  

2. **Scenario:** Intento de cierre con paradas pendientes
   - **Given:** El Conductor intenta cerrar la ruta pero existen paradas en estado "pendiente".
   - **When:** Solicita el cierre de la ruta.
   - **Then:** El sistema informa cuáles paradas están con estado "pendiente", advierte que si no las gestiona el sistema cerrará la ruta automáticamente al vencer el tiempo límite de 2 días en tránsito, y le da la opción de gestionar las pendientes o confirmar el cierre manual asumiendo que el sistema marcará las restantes como "sin_gestion_conductor" y la ruta cambia a estado "cerrada".El estado del vehículo pasa a "disponible" y el estado del conductor a "activo".

3. **Scenario:** Cierre automático por tiempo excedido
   - **Given:** La ruta lleva más de 2 días en estado "en_transito" sin que el conductor haya ejecutado el cierre manual y existen paradas con estado "pendiente".
   - **When:** El sistema detecta que se superaron los 2 días en tránsito.
   - **Then:** El sistema cambia las paradas con estado "pendiente" a "sin_gestion_conductor" con origen "sistema", cambia el estado de la ruta a "cerrada_automatica", genera y envía el evento "cierre_ruta" al Sistema de Facturación y Liquidación con tipo de cierre automático, envia el evento "paradas_sin_gestionar" al Sistema de Gestión de Paquetes el estado final de cada paquete y envía alerta de alta prioridad al Despachador Logístico.El estado del vehículo pasa a "disponible" y el estado del conductor a "activo".

---

## Edge Cases

- **¿Qué ocurre cuando el conductor intenta registrar una entrega exitosa sin adjuntar foto de evidencia (POD)?**  
  El sistema rechaza el registro e indica que la foto POD es obligatoria para confirmar una entrega exitosa. El conductor debe adjuntarla antes de continuar.

- **¿Puede el Despachador Logístico forzar el cierre de una ruta si el conductor no lo hace dentro del tiempo establecido?**  
  Sí. El Despachador puede forzar el cierre desde su panel. El cierre forzado se registra con `tipo_cierre: forzado_despachador` y el sistema marca todas las paradas "pendiente" como "sin_gestion_conductor" con origen "sistema".

- **¿Qué ocurre cuando el conductor cierra la ruta sin conexión a internet?**  
  El sistema almacena la acción localmente y ejecuta el cierre formal (incluyendo el envío de eventos a los sistemas externos) al recuperar conectividad. El timestamp de cierre corresponde al momento en que el conductor ejecutó la acción, no al de la sincronización.

---

## Functional Requirements

| ID | Requisito                                                                                                                                                                                          |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **FR-013** | El sistema DEBE permitir al Conductor registrar el resultado de cada parada: "exitosa", "fallida" con motivo, o "novedad" grave.                                                                   |
| **FR-014** | El sistema DEBE notificar al Sistema de Gestión de Paquetes en tiempo real el estado de cada parada ("exitosa", parada "fallida" con motivo, "novedad" grave).                                     |
| **FR-015** | El sistema DEBE enviar el evento `ruta_cerrada` al Sistema de Facturación y Liquidación al cerrar una ruta.                                                                                        |
| **FR-017** | El sistema DEBE cerrar automáticamente las rutas que superen 2 días en estado "en_transito" sin cierre manual del conductor.                                                                       |
| **FR-018** | El sistema DEBE marcar las paradas en estado "pendiente" a estado "sin_gestion_conductor" y origen "sistema" al momento del cierre automático o forzado.                                           |
| **FR-019** | El sistema DEBE notificar al Sistema de Gestión de Paquetes el estado final de cada parada que no fue gestionada al momento en que se hizo un cierre de ruta, ya sea automático o forzado.         |
| **FR-020** | El sistema NO DEBE permitir que el conductor interactúe directamente con el Sistema de Facturación y Liquidación. El envío del evento de cierre es responsabilidad exclusiva del sistema de rutas. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-017** | El Sistema de Gestión de Paquetes recibe el evento `paquete_entregado`, `paquete_fallido` o `paquete_novedad` para que cambie el estado del paquete en tiempo real conforme el conductor registra cada parada. |
| **SC-018** | El Sistema de Facturación y Liquidación recibe el evento `ruta_cerrada` con la estructura completa al momento del cierre, ya sea manual, automático o forzado. |
| **SC-019** | Las paradas en estado "pendiente" al momento del cierre quedan registradas como "sin_gestion_conductor" y origen "sistema", diferenciándolas claramente de las novedades registradas por el conductor para efectos de liquidación. |
| **SC-020** | El sistema no pierde ningún registro de parada cuando el conductor opera sin conexión; todos se sincronizan correctamente al recuperar conectividad. |
