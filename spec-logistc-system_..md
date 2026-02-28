# Feature Specification: Planificación y Gestión de Rutas y Flota

**Created:** 21/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 1 – Planificar Rutas (Priority: P1)

**Como** Sistema de Gestión de Paquetes,  
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

## User Story 2 – Despachar Paquetes a Rutas (Priority: P1)

**Como** Despachador Logístico,  
**quiero** confirmar que los paquetes están cargados en el vehículo y despachar los paquetes asignados a la ruta,  
**para que** los paquetes avancen al estado 'En Tránsito', el conductor reciba la ruta en su dispositivo y el Sistema de Gestión de Paquetes sea notificado del cambio de estado.

**Why this priority:** Es el punto de activación de la operación de entrega y el evento que notifica al Sistema de Gestión de Paquetes que los paquetes están en camino. Sin el despacho, ninguna ruta planificada se ejecuta.

**Independent Test:** Se puede probar tomando una ruta planificada, ejecutando el despacho y verificando que el conductor recibe la ruta en su dispositivo, los paquetes cambian a 'En Tránsito' y el Sistema de Gestión de Paquetes recibe la notificación.

**Acceptance Scenarios:**

1. **Scenario:** Despacho exitoso
   - **Given:** Una ruta tiene paquetes asignados, vehículo con capacidad adecuada y conductor operativo disponible.
   - **When:** El Despachador Logístico confirma el despacho.
   - **Then:** La ruta y los paquetes pasan a estado 'En Tránsito', el conductor recibe la ruta en su dispositivo y el Sistema de Gestión de Paquetes es notificado para actualizar el estado de cada paquete a 'En Tránsito'.

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
   - **Then:** El paquete con novedad queda excluido de la ruta, cambia al estado correspondiente y se registra el motivo. La ruta se despacha con los paquetes restantes y el Sistema de Gestión de Paquetes es notificado del cambio de estado de cada paquete.

---

## User Story 3 – Gestionar Vehículos de la Flota (Priority: P1)

**Como** Administrador de Flota,  
**quiero** registrar, actualizar y dar de baja vehículos con todos sus atributos operativos,  
**para que** el algoritmo de planificación cuente con información precisa y actualizada de la capacidad disponible.

**Why this priority:** Sin un catálogo de vehículos actualizado, el algoritmo de consolidación de carga no puede funcionar. Es prerequisito de todas las demás historias.

**Independent Test:** Se puede probar registrando un vehículo y verificando que aparece como opción válida en el algoritmo de planificación con su capacidad correctamente reflejada.

**Acceptance Scenarios:**

1. **Scenario:** Registro exitoso
   - **Given:** El Administrador tiene sesión activa y el vehículo no existe en el sistema.
   - **When:** Registra el vehículo con sus atributos operativos completos (placa, tipo, modelo, capacidad de peso, volumen máximo y zona de operación).
   - **Then:** El vehículo queda registrado como 'Disponible', visible para el algoritmo de planificación y el sistema confirma el registro con un identificador único.

2. **Scenario:** Registro bloqueado — placa duplicada
   - **Given:** Existe un vehículo con una placa ya registrada en el sistema.
   - **When:** El Administrador intenta registrar otro vehículo con la misma placa.
   - **Then:** El sistema rechaza el registro, informa el error y conserva los datos ingresados para su corrección.

3. **Scenario:** Actualización bloqueada — vehículo en tránsito
   - **Given:** Existe un vehículo con una ruta activa en curso.
   - **When:** El Administrador intenta modificar sus atributos operativos.
   - **Then:** El sistema rechaza la actualización, informa la causa y el registro permanece sin cambios.

4. **Scenario:** Baja de vehículo sin actividad activa
   - **Given:** Existe un vehículo en estado 'Disponible' sin rutas ni paquetes activos asignados.
   - **When:** El Administrador solicita dar de baja el vehículo.
   - **Then:** El vehículo pasa a estado 'Inactivo', queda excluido del algoritmo de planificación y se conserva su historial para trazabilidad.

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

## User Story 7 – Registrar Resultado de Parada (Priority: P2)

**Como** Conductor,  
**quiero** registrar el resultado de cada parada, ya sea una entrega exitosa, una parada fallida o una novedad grave,  
**para que** el Sistema de Gestión de Paquetes se mantenga actualizado con el estado de cada paquete en tiempo real y los datos queden registrados para el cierre de ruta.

**Why this priority:** Es el núcleo operativo del módulo. El conductor genera los datos de cada parada; el sistema los clasifica, notifica al Sistema de Gestión de Paquetes en tiempo real y los acumula para el cierre de ruta.

**Independent Test:** Se puede probar registrando una entrega exitosa y una parada fallida en una ruta activa, y verificando que el Sistema de Gestión de Paquetes actualiza el estado de cada paquete correctamente.

**Acceptance Scenarios:**

1. **Scenario:** Registro de entrega exitosa
   - **Given:** El Conductor se encuentra en la parada de entrega con el paquete correspondiente.
   - **When:** Registra la entrega con foto y firma como evidencia (POD).
   - **Then:** La parada cambia a estado 'Exitosa', el Sistema de Gestión de Paquetes es notificado para actualizar el paquete a 'Entregado' y el dato queda registrado para el cierre de ruta.

2. **Scenario:** Registro de parada fallida
   - **Given:** El Conductor se encuentra en la parada pero no puede completar la entrega.
   - **When:** Registra la parada como fallida con el motivo de no entrega correspondiente (cliente ausente, dirección incorrecta, rechazado por cliente o zona de difícil acceso).
   - **Then:** La parada cambia a estado 'Fallida' con su motivo registrado, el Sistema de Gestión de Paquetes es notificado para que actualice el estado del paquete y tome la decisión correspondiente, y el dato queda registrado para el cierre de ruta.

3. **Scenario:** Registro de novedad grave en ruta
   - **Given:** El Conductor detecta durante la ruta que un paquete está dañado, fue extraviado o requiere devolución.
   - **When:** Registra la novedad grave con el tipo de novedad correspondiente.
   - **Then:** El Sistema de Gestión de Paquetes es notificado de forma inmediata para que active el protocolo correspondiente (seguro o devolución al origen) y el evento queda registrado para el cierre de ruta.

---

## User Story 8 – Cerrar Ruta (Priority: P1)

**Como** Conductor,  
**quiero** cerrar la ruta una vez finalizadas todas las paradas,  
**para que** el sistema genere el informe de cierre consolidado y lo envíe al Sistema de Facturación y Liquidación para que calcule la liquidación correspondiente.

**Why this priority:** Es el paso final del flujo operativo. Sin el cierre, el Sistema de Facturación y Liquidación no puede calcular la liquidación del conductor.

**Independent Test:** Se puede probar completando todas las paradas de una ruta, ejecutando el cierre y verificando que el Sistema de Facturación y Liquidación recibe el evento `ruta_cerrada` con la estructura correcta.

**Acceptance Scenarios:**

1. **Scenario:** Cierre exitoso con todas las paradas gestionadas
   - **Given:** El Conductor ha gestionado todas las paradas de la ruta con su respectivo estado (exitosa, fallida o novedad).
   - **When:** Solicita el cierre de la ruta.
   - **Then:** La ruta cambia a estado 'Cerrada', el sistema genera y envía al Sistema de Facturación y Liquidación el evento.

2. **Scenario:** Intento de cierre con paradas pendientes
   - **Given:** El Conductor intenta cerrar la ruta pero existen paradas sin gestionar.
   - **When:** Solicita el cierre de la ruta.
   - **Then:** El sistema informa cuáles paradas están pendientes, advierte que si no las gestiona el sistema cerrará la ruta automáticamente al vencer el tiempo límite, y le da la opción de gestionar las pendientes o confirmar el cierre manual asumiendo que el sistema marcará las restantes como novedad automática.

3. **Scenario:** Cierre automático por tiempo excedido
   - **Given:** La ruta lleva activa más tiempo del permitido sin que el conductor haya ejecutado el cierre manual y existen paradas sin gestionar.
   - **When:** El sistema detecta que se superó el tiempo límite configurado.
   - **Then:** El sistema marca todas las paradas sin gestionar con estado `sin_gestión_conductor` y origen `sistema`, cambia el estado de la ruta a 'Cerrada (Automático)', genera y envía el evento `ruta_cerrada` al Sistema de Facturación y Liquidación con `tipo_cierre: automático`, notifica al Sistema de Gestión de Paquetes el estado final de cada paquete y envía alerta de alta prioridad al Despachador.

---

## Edge Cases

- ¿Qué ocurre si el conductor es dado de baja mientras tiene una ruta activa en tránsito?
- ¿Puede el Despachador Logístico forzar el cierre de una ruta si el conductor no lo hace en un tiempo determinado? `[NEEDS CLARIFICATION: definir tiempo límite y rol autorizado]`
- ¿Qué ocurre si el Sistema de Facturación y Liquidación no está disponible al momento del cierre de ruta? ¿Se reintenta automáticamente o queda en cola? `[NEEDS CLARIFICATION: definir comportamiento de reintento]`
- ¿Cuántos reintentos puede registrar el conductor en una misma parada antes de que el sistema la cierre como fallida definitivamente? `[NEEDS CLARIFICATION: definir límite de reintentos]`
- ¿Puede el Despachador modificar el orden de paradas de una ruta ya despachada y en tránsito?
- ¿Con qué frecuencia se actualiza el panel de disponibilidad de la flota? `[NEEDS CLARIFICATION: definir SLA de sincronización]`
- ¿Qué pasa si el Sistema de Gestión de Paquetes no está disponible al momento de notificar un cambio de estado de parada? `[NEEDS CLARIFICATION: definir comportamiento de reintento]`

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-M2-001** | El sistema DEBE solicitar automáticamente una ruta cuando un paquete alcanza el estado 'Listo para Despacho'. |
| **FR-M2-002** | El sistema DEBE asignar el paquete a una ruta existente si la zona geográfica coincide y tiene capacidad disponible. |
| **FR-M2-003** | El sistema DEBE generar una nueva ruta cuando no existe una ruta compatible o la existente ha alcanzado el 90% de su capacidad. |
| **FR-M2-004** | El sistema DEBE seleccionar el vehículo de menor capacidad disponible que soporte el peso del conjunto de paquetes asignados. |
| **FR-M2-005** | El sistema DEBE bloquear el despacho si el peso total de los paquetes supera el umbral permitido del vehículo asignado. |
| **FR-M2-006** | El sistema DEBE permitir al Administrador de Flota registrar, actualizar y dar de baja vehículos de la flota. |
| **FR-M2-007** | El sistema DEBE impedir el registro de vehículos con placa duplicada. |
| **FR-M2-008** | El sistema DEBE impedir modificaciones a vehículos con rutas activas en curso. |
| **FR-M2-009** | El sistema DEBE permitir al Administrador de Flota asignar un conductor activo a un vehículo en estado 'Disponible'. |
| **FR-M2-010** | El sistema DEBE impedir que un conductor sea asignado a más de un vehículo activo simultáneamente. |
| **FR-M2-011** | El sistema DEBE impedir la reasignación de conductor en vehículos con rutas activas. |
| **FR-M2-012** | El sistema DEBE registrar el historial de asignaciones conductor-vehículo con fecha y hora de inicio y fin. |
| **FR-M2-013** | El sistema DEBE permitir al Conductor registrar el resultado de cada parada: exitosa, fallida con motivo, o novedad grave. |
| **FR-M2-014** | El sistema DEBE notificar al Sistema de Gestión de Paquetes en tiempo real el estado de cada parada (entrega exitosa, parada fallida con motivo, novedad grave). |
| **FR-M2-015** | El sistema DEBE enviar el evento `ruta_cerrada` al Sistema de Facturación y Liquidación al cerrar una ruta. |
| **FR-M2-016** | El sistema DEBE notificar al Despachador Logístico ante cualquier bloqueo en la planificación o despacho de rutas. |
| **FR-M2-017** | El sistema DEBE cerrar automáticamente las rutas que superen el tiempo máximo de operación configurado. `[NEEDS CLARIFICATION: definir valor por defecto]` |
| **FR-M2-018** | El sistema DEBE marcar con estado `sin_gestión_conductor` y origen `sistema` las paradas sin gestionar al momento del cierre automático. |
| **FR-M2-019** | El sistema DEBE notificar al Sistema de Gestión de Paquetes el estado final de cada paquete al cierre de ruta, ya sea manual o automático. |
| **FR-M2-020** | El sistema NO DEBE permitir que el conductor interactúe directamente con el Sistema de Facturación y Liquidación. El envío del evento de cierre es responsabilidad exclusiva del sistema de rutas. |

---

## Key Entities

| Entidad | Descripción |
|---|---|
| **Ruta** | Recorrido asignado a un vehículo con un conjunto de paradas ordenadas geográficamente. Atributos: ID, estado, zona, vehículo asignado, conductor, fecha/hora inicio y fin, tipo de cierre, lista de paradas. |
| **Vehículo** | Unidad de transporte tipificada con capacidad de peso y volumen. Atributos: ID, placa, tipo (Moto, Van, NHR, Turbo), modelo, capacidad de peso (kg), volumen máximo (m³), zona de operación, estado, conductor asignado. |
| **Conductor** | Operario habilitado para operar un tipo de vehículo. Atributos: ID, nombre, estado, turno activo, vehículo asignado. |
| **Parada** | Punto de entrega dentro de una ruta asociado a un paquete. Atributos: id del paquete, fecha/hora de gestión, dirección, coordenadas GPS, estado final (entregado, fallido_cliente, fallido_conductor, dañado), motivo de novedad, evidencia de entrega (foto + firma), origen (conductor o sistema). |
| **Evento ruta_cerrada** | Payload enviado al Sistema de Facturación y Liquidación al cierre de ruta. Contiene: fecha/hora de cierre, datos de la ruta, vehículo, conductor y el detalle completo de cada parada con su estado final y motivo. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | El sistema asigna o genera una ruta para un paquete en estado 'Listo para Despacho' sin intervención manual. |
| **SC-002** | El algoritmo selecciona el vehículo óptimo de menor capacidad disponible que cumpla el requerimiento de peso. |
| **SC-003** | El Conductor recibe la ruta en su dispositivo inmediatamente después de que el Despachador confirma el despacho. |
| **SC-004** | El Sistema de Gestión de Paquetes recibe la notificación de cambio de estado de cada paquete en tiempo real conforme el conductor registra cada parada. |
| **SC-005** | El Sistema de Facturación y Liquidación recibe el evento `ruta_cerrada` con la estructura completa al momento del cierre, ya sea manual o automático. |
| **SC-006** | El Administrador de Flota puede registrar un vehículo y verlo disponible para el algoritmo de planificación en menos de 2 minutos. |
