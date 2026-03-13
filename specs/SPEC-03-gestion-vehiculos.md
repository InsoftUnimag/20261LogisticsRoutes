# Feature Specification: Gestión de Vehículos de la Flota (MOD2-UC-003)

**Created:** 27/02/2026  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 3 – Gestionar Vehículos de la Flota (Priority: P1)

**Como** Administrador de Flota,  
**quiero** registrar, actualizar y dar de baja vehículos con todos sus atributos operativos,  
**para que** el algoritmo de planificación cuente con información precisa y actualizada de la capacidad disponible.

**Why this priority:** Sin un catálogo de vehículos actualizado, el algoritmo de consolidación de carga no puede funcionar. Es prerequisito de todas las demás historias.

**Independent Test:** Se puede probar registrando un vehículo y verificando que aparece como opción válida en el algoritmo de planificación con su capacidad correctamente reflejada.

---

## Acceptance Scenarios

1. **Scenario:** Registro exitoso
   - **Given:** El Administrador tiene sesión activa y el vehículo no existe en el sistema.
   - **When:** Registra el vehículo con sus atributos operativos completos (placa, tipo, modelo, capacidad de peso, volumen máximo y zona de operación).
   - **Then:** El vehículo queda registrado como "disponible", visible para el algoritmo de planificación y el sistema confirma el registro con un identificador único.

2. **Scenario:** Registro bloqueado — placa duplicada
   - **Given:** Existe un vehículo con una placa ya registrada en el sistema.
   - **When:** El Administrador intenta registrar otro vehículo con la misma placa.
   - **Then:** El sistema rechaza el registro, informa el error y conserva los datos ingresados para su corrección.

3. **Scenario:** Actualización bloqueada — vehículo en tránsito
   - **Given:** Existe un vehículo con una ruta activa en curso.
   - **When:** El Administrador intenta modificar sus atributos operativos.
   - **Then:** El sistema rechaza la actualización, informa la causa y el registro permanece sin cambios.

4. **Scenario:** Baja de vehículo sin actividad activa
   - **Given:** Existe un vehículo en estado "disponible" sin rutas ni paquetes activos asignados.
   - **When:** El Administrador solicita dar de baja el vehículo.
   - **Then:** El vehículo pasa a estado "inactivo", queda excluido del algoritmo de planificación y se conserva su historial para trazabilidad.

---

## Edge Cases

- **¿Qué ocurre cuando se intenta dar de baja un vehículo que tiene una ruta en estado "lista_para_despacho" o "confirmada"?**  
  El sistema bloquea la baja e informa al Administrador que el vehículo tiene una ruta pendiente. La baja solo puede realizarse una vez que la ruta haya sido despachada y cerrada.

- **¿Qué ocurre cuando se actualiza la capacidad de peso de un vehículo que ya tiene paquetes asignados en una ruta pendiente de despacho?**  
  El sistema recalcula si los paquetes asignados siguen dentro del umbral permitido con la nueva capacidad. Si hay sobrepeso, alerta al Despachador Logístico para que ajuste los paquetes o reasigne el tipo de vehículo antes del despacho.

- **¿Cómo maneja el sistema el intento de registrar un vehículo con capacidad de peso igual a 0 o negativa?**  
  El sistema rechaza el registro con un mensaje de error de validación indicando que la capacidad de peso debe ser un valor positivo mayor a cero.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-009** | El sistema DEBE permitir al Administrador de Flota registrar, actualizar y dar de baja vehículos de la flota. |
| **FR-010** | El sistema DEBE impedir el registro de vehículos con placa duplicada. |
| **FR-011** | El sistema DEBE impedir modificaciones o baja de vehículos con rutas activas en curso. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-007** | El Administrador de Flota puede registrar un vehículo y verlo disponible para el algoritmo de planificación en menos de 2 minutos. |
| **SC-008** | El sistema rechaza registros con placa duplicada o datos inválidos sin pérdida de los datos ingresados por el Administrador. |
| **SC-009** | Un vehículo dado de baja queda inmediatamente excluido del algoritmo de planificación y sus datos históricos se conservan para consulta. |
