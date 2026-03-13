# Feature Specification: Despachar Paquetes a Rutas (MOD2-UC-002)

**Created:** 2026-03-01  
**By:** Esteban Puello, Jose Rodriguez, Laura Perez, Robert Gonzalez

---

## User Story 2 – Despachar Paquetes a Rutas (Priority: P1)

**Como** Despachador Logístico,  
**quiero** revisar las rutas en estado "lista_para_despacho" y confirmar cada una,  
**para que** el sistema asigne el conductor y vehículo físico, y la ruta quede lista para que el conductor inicie el tránsito.

**Why this priority:** Es el punto de activación de la operación de entrega. Sin la confirmación del despachador, ninguna ruta planificada puede pasar a manos del conductor.

**Independent Test:** Se puede probar tomando una ruta en estado "lista_para_despacho", ejecutando la confirmación y verificando que la ruta pasa a "confirmada", el conductor y vehículo físico quedan asignados y el conductor puede ver la ruta en su dispositivo.

---

## Acceptance Scenarios

1. **Scenario:** Confirmación exitosa de despacho
   - **Given:** Una ruta se encuentra en estado "lista_para_despacho" con conductor "activo" y vehículo físico "disponible" del tipo requerido.
   - **When:** El Despachador Logístico confirma el despacho.
   - **Then:** El sistema asigna el conductor y el vehículo físico específico a la ruta, optimiza el orden de paradas, genera el manifiesto y la ruta pasa a estado "confirmada". El conductor recibe la ruta en su dispositivo.

2. **Scenario:** Confirmación bloqueada — conductor no disponible
   - **Given:** Una ruta está en estado "lista_para_despacho" pero el conductor asignado dejó de estar "activo".
   - **When:** El Despachador intenta confirmar el despacho.
   - **Then:** El sistema bloquea la confirmación, informa la causa y sugiere reasignar un conductor habilitado antes de continuar.

3. **Scenario:** Confirmación bloqueada — vehículo físico no disponible
   - **Given:** Una ruta está en estado "lista_para_despacho" pero no hay vehículo físico "disponible" del tipo requerido al momento de confirmar.
   - **When:** El Despachador intenta confirmar el despacho.
   - **Then:** El sistema bloquea la confirmación, informa que no hay unidades disponibles del tipo requerido y queda a la espera de que el Despachador resuelva la situación.

4. **Scenario:** Exclusión de paquete por novedad física antes de confirmar
   - **Given:** Una ruta está en estado "lista_para_despacho" y el Despachador identifica que un paquete tiene una novedad física antes de confirmar.
   - **When:** El Despachador excluye el paquete y confirma el despacho con los restantes.
   - **Then:** El paquete excluido cambia al estado correspondiente con el motivo registrado. La ruta se confirma con los paquetes restantes y pasa a estado "confirmada".

---

## Edge Cases

- **¿Qué pasa si entre el momento en que la ruta pasa a "lista_para_despacho" y el momento en que el Despachador la revisa, llega un paquete nuevo de la misma zona?**  
  Una ruta en estado "lista_para_despacho" no debe aceptar nuevos paquetes. El paquete nuevo debe crear una ruta distinta para esa zona.

- **¿Puede el Despachador reasignar manualmente el conductor o el vehículo físico de una ruta?**  
  Sí, el despachador puede modificar detalles de la ruta si lo considera necesario.

---

## Functional Requirements

| ID | Requisito |
|---|---|
| **FR-M2-009** | El sistema DEBE mostrar al Despachador Logístico todas las rutas en estado "lista_para_despacho" con su detalle: zona, cantidad de paquetes, peso acumulado y tipo de vehículo requerido. |
| **FR-M2-010** | El sistema DEBE permitir al Despachador confirmar cada ruta en estado "lista_para_despacho". |
| **FR-M2-011** | Al confirmar el despacho, el sistema DEBE asignar el vehículo físico "disponible" del tipo requerido y el conductor "activo" a la ruta. |
| **FR-M2-012** | Al confirmar el despacho, el sistema DEBE optimizar el orden de paradas y generar el manifiesto de ruta para el conductor. |
| **FR-M2-013** | Al confirmar el despacho, la ruta DEBE pasar a estado "confirmada" y el conductor DEBE recibir la ruta en su dispositivo. |
| **FR-M2-014** | El sistema DEBE bloquear la confirmación si no hay conductor "activo" o vehículo físico "disponible" del tipo requerido, informando la causa. |
| **FR-M2-015** | El sistema DEBE permitir al Despachador excluir paquetes con novedades físicas antes de confirmar el despacho, registrando el motivo de exclusión. |
| **FR-M2-016** | Una ruta en estado "lista_para_despacho" NO DEBE aceptar nuevos paquetes. |

---

## Key Entities

| Entidad | Atributos relevantes a esta historia |
|---|---|
| **Ruta** | `ruta_id`, `estado` ("lista_para_despacho" → "confirmada"), `tipo_vehiculo_requerido`, `vehiculo_asignado` (se asigna al confirmar), `conductor_asignado` (se asigna al confirmar), `lista_paquetes`. |
| **Vehículo** | `vehiculo_id`, `placa`, `tipo`, `estado` ("disponible" / "en_transito" / "inactivo"). |
| **Conductor** | `conductor_id`, `nombre`, `estado` ("activo" / "inactivo" / "en_ruta"), `vehiculo_asignado`. |

---

## Success Criteria

| ID | Criterio |
|---|---|
| **SC-001** | Al confirmar el despacho, la ruta pasa a "confirmada" y el conductor recibe la ruta en su dispositivo en menos de 5 segundos. |
| **SC-002** | El sistema bloquea el 100% de las confirmaciones cuando no hay conductor "activo" o vehículo físico "disponible" del tipo requerido. |
