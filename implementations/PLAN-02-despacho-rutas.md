# Implementation Plan: Despacho de Rutas (MOD2-UC-002)

**Date:** 2026-04-06
**Spec:** [SPEC-02-despacho-rutas.md](../specs/SPEC-02-despacho-rutas.md)

---

## Summary

El Despachador Logístico revisa las rutas en estado `LISTA_PARA_DESPACHO`, asigna conductor y vehículo físico disponibles, optimiza el orden de paradas, genera el manifiesto y confirma el despacho. La ruta pasa a `CONFIRMADA` y el conductor recibe la ruta en su dispositivo. El sistema bloquea la confirmación si no hay conductor `activo` o vehículo `disponible` del tipo requerido.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 17 |
| **Framework** | Spring Boot 3.x |
| **Primary Dependencies** | Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok |
| **Storage** | PostgreSQL |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Target Platform** | Linux server / Backend REST API |
| **Performance Goals** | Ruta confirmada y conductor notificado en < 5 segundos |
| **Constraints** | Bloquear confirmación si no hay conductor activo o vehículo disponible del tipo requerido. Ruta en `LISTA_PARA_DESPACHO` no acepta nuevos paquetes. |
| **Scale/Scope** | Módulo 2 — endpoints de despacho para el Despachador Logístico |

---

## Project Structure

> Este plan extiende la estructura creada en PLAN-01. Se agregan los componentes de despacho.

```
src/
├── main/
│   └── java/com/logistics/routes/
│       ├── model/
│       │   ├── Ruta.java           (ya existe — se extiende con vehiculoId, conductorId)
│       │   ├── Vehiculo.java       [NUEVO]
│       │   └── Conductor.java      [NUEVO]
│       ├── repository/
│       │   ├── RutaRepository.java (ya existe)
│       │   ├── VehiculoRepository.java  [NUEVO]
│       │   └── ConductorRepository.java [NUEVO]
│       ├── service/
│       │   └── DespachoService.java     [NUEVO]
│       ├── controller/
│       │   └── DespachoController.java  [NUEVO]
│       └── dto/
│           ├── ConfirmarDespachoRequest.java  [NUEVO]
│           └── RutaDetalleResponse.java       [NUEVO]
└── test/
    └── java/com/logistics/routes/
        ├── service/
        │   └── DespachoServiceTest.java       [NUEVO]
        └── integration/
            └── DespachoIntegrationTest.java   [NUEVO]
```

**Structure Decision:** Se extiende el mismo proyecto Spring Boot de PLAN-01. Depende de los modelos `Ruta`, `EstadoRuta` ya creados en la fase Foundational de PLAN-01.

---

## Phase 1: Setup

> **Dependencia:** PLAN-01 Phase 1 y Phase 2 (Foundational) deben estar completas.

- [ ] T001 Verificar que las entidades `Ruta`, `EstadoRuta`, `TipoVehiculo` de PLAN-01 están disponibles y compilan correctamente

---

## Phase 2: Foundational — Modelos de Vehículo y Conductor

**Purpose:** Entidades de flota necesarias para el flujo de despacho. Bloquea todas las historias de este plan.

⚠️ **CRÍTICO: No se puede implementar ninguna historia de despacho sin estos modelos.**

- [ ] T002 [P] Crear enum `EstadoVehiculo`: `DISPONIBLE`, `EN_TRANSITO`, `INACTIVO`
- [ ] T003 [P] Crear enum `EstadoConductor`: `ACTIVO`, `INACTIVO`, `EN_RUTA`
- [ ] T004 [P] Crear entidad `Vehiculo` en `model/Vehiculo.java`: `id` (UUID), `placa` (String, único), `tipo` (TipoVehiculo), `modelo` (String), `capacidadPesoKg` (float), `volumenMaximoM3` (float), `zonaOperacion` (String), `estado` (EstadoVehiculo), `conductorId` (UUID, nullable)
- [ ] T005 [P] Crear entidad `Conductor` en `model/Conductor.java`: `id` (UUID), `nombre` (String), `estado` (EstadoConductor), `vehiculoAsignadoId` (UUID, nullable)
- [ ] T006 [P] Crear `VehiculoRepository` con query: `findByTipoAndEstado(TipoVehiculo tipo, EstadoVehiculo estado)`
- [ ] T007 [P] Crear `ConductorRepository` con query: `findByEstado(EstadoConductor estado)`
- [ ] T008 [P] Crear DTOs: `ConfirmarDespachoRequest` (con `conductorId`, `vehiculoId` opcionales para reasignación manual) y `RutaDetalleResponse` (con zona, paquetes, peso, tipo de vehículo requerido)

**Checkpoint: Modelos de flota listos. Se puede iniciar implementación de historias de despacho.**

---

## Phase 3: User Story 2 — Listar rutas listas para despacho (Priority: P1)

**Goal:** El Despachador puede consultar todas las rutas en estado `LISTA_PARA_DESPACHO` con su detalle.

**Independent Test:** Llamar `GET /api/despacho/rutas` y verificar que retorna solo rutas en estado `LISTA_PARA_DESPACHO` con detalle correcto.

### Tests para US2 — Consulta
- [ ] T009 [P] [US2] Test unitario: `DespachoServiceTest` — listar rutas devuelve solo las que están en `LISTA_PARA_DESPACHO`
- [ ] T010 [US2] Test unitario: listar retorna lista vacía cuando no hay rutas en ese estado

### Implementación para US2 — Consulta
- [ ] T011 [P] [US2] Agregar query en `RutaRepository`: `findByEstado(EstadoRuta estado)`
- [ ] T012 [US2] Implementar `DespachoService.listarRutasParaDespacho()` — retorna lista de `RutaDetalleResponse`
- [ ] T013 [US2] Implementar `DespachoController.listarRutas()` — `GET /api/despacho/rutas`

**Checkpoint: El Despachador puede ver todas las rutas pendientes de despacho.**

---

## Phase 4: User Story 2 — Confirmar despacho (Priority: P1)

**Goal:** El Despachador confirma el despacho: el sistema valida disponibilidad, asigna conductor y vehículo, optimiza paradas, genera manifiesto y transiciona la ruta a `CONFIRMADA`.

**Independent Test:** Tener una ruta en `LISTA_PARA_DESPACHO`, un conductor `ACTIVO` y un vehículo `DISPONIBLE` del tipo requerido. Llamar `POST /api/despacho/rutas/{id}/confirmar` y verificar transición a `CONFIRMADA`.

### Tests para US2 — Confirmación
- [ ] T014 [P] [US2] Test unitario: `DespachoServiceTest` — confirmación exitosa con conductor activo y vehículo disponible
- [ ] T015 [P] [US2] Test unitario: confirmación bloqueada cuando conductor no está `ACTIVO`
- [ ] T016 [P] [US2] Test unitario: confirmación bloqueada cuando no hay vehículo `DISPONIBLE` del tipo requerido
- [ ] T017 [US2] Test unitario: exclusión de paquete con novedad antes de confirmar
- [ ] T018 [US2] Test de integración: `DespachoIntegrationTest` — flujo completo de confirmación con Testcontainers

### Implementación para US2 — Confirmación
- [ ] T019 [P] [US2] Implementar `DespachoService.confirmarDespacho(UUID rutaId, ConfirmarDespachoRequest)`:
  - Verificar que la ruta está en `LISTA_PARA_DESPACHO`; si no, lanzar excepción
  - Verificar que hay conductor `ACTIVO` disponible; si no, bloquear con mensaje
  - Verificar que hay vehículo `DISPONIBLE` del tipo requerido; si no, bloquear con mensaje
  - Asignar `conductorId` y `vehiculoId` a la ruta
  - Invocar `optimizarOrdenParadas(ruta)` — ordenar paradas por coordenadas geográficas
  - Generar manifiesto (log o DTO estructurado como placeholder)
  - Transicionar ruta a `CONFIRMADA`
  - Actualizar estado del vehículo a `EN_TRANSITO` y del conductor a `EN_RUTA`
  - Notificar al conductor (stub de `NotificacionService`)
- [ ] T020 [US2] Implementar método privado `optimizarOrdenParadas(Ruta ruta)` — algoritmo básico de ordenamiento por proximidad geográfica (nearest neighbor)
- [ ] T021 [US2] Implementar `DespachoController.confirmarDespacho()` — `POST /api/despacho/rutas/{id}/confirmar`
- [ ] T022 [US2] Implementar `DespachoService.excluirPaquete(UUID rutaId, UUID paqueteId, String motivo)` para excluir paquetes antes de confirmar
- [ ] T023 [US2] Implementar `DespachoController.excluirPaquete()` — `DELETE /api/despacho/rutas/{id}/paquetes/{paqueteId}`

**Checkpoint: El Despachador puede confirmar despachos completos, con bloqueos correctos y exclusiones.**

---

## Phase N: Polish y Concerns Transversales

- [ ] T024 Documentar endpoints de despacho con Javadoc
- [ ] T025 Revisar logs estructurados para auditoría de confirmaciones
- [ ] T026 Agregar validaciones de entrada en DTOs con `@Valid`
- [ ] T027 Revisar manejo de excepciones y respuestas HTTP apropiadas (404, 409, 422)

---

## Dependencies & Execution Order

```
PLAN-01 Phase 1 + Phase 2 (Foundational)
    └── Este plan — Phase 1 (verificación)
            └── Phase 2 (Foundational Vehículo/Conductor — BLOQUEA)
                    ├── Phase 3 (Listar rutas) — puede ir primero
                    └── Phase 4 (Confirmar despacho) — depende de Phase 3

Phase N (Polish) — al final
```

---

## Notes

- `optimizarOrdenParadas` puede implementarse inicialmente con un algoritmo simple de nearest neighbor. La optimización avanzada (TSP) es un refinamiento posterior.
- La notificación al conductor es un stub en este plan; se conectará a un mecanismo real (WebSocket push, FCM) en un plan de integración de frontend.
- No agregar paquetes a rutas en estado `LISTA_PARA_DESPACHO` — la restricción debe aplicarse también en `PlanificacionService` de PLAN-01.
- Verificar tests y hacer commit al terminar cada tarea o grupo lógico.
