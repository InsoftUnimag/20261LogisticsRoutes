# Implementation Plan: Gestión de Vehículos y Conductores (MOD2-UC-003 / MOD2-UC-004 / MOD2-UC-005)

**Date:** 2026-04-06
**Specs:**
- [SPEC-03-gestion-vehiculos.md](../specs/SPEC-03-gestion-vehiculos.md)
- [SPEC-04-asignacion-conductores.md](../specs/SPEC-04-asignacion-conductores.md)
- [SPEC-05-disponibilidad-flota.md](../specs/SPEC-05-disponibilidad-flota.md)

---

## Summary

Este plan agrupa tres SPECs que comparten las mismas entidades (`Vehiculo`, `Conductor`) y son gestionadas por el mismo actor (Administrador de Flota). Cubre: registro, actualización y baja de vehículos con validaciones (placa única, bloqueo si en tránsito); asignación y desvinculación de conductores a vehículos con historial de asignaciones; y consulta en tiempo real del panel de disponibilidad de la flota.

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
| **Performance Goals** | Vehículo registrado visible al algoritmo en < 2 minutos. Panel de flota actualizado en tiempo real con cada cambio de estado. |
| **Constraints** | Placa única. Bloqueo de modificación/baja en vehículos con rutas activas. Conductor no puede estar en más de un vehículo activo simultáneamente. |
| **Scale/Scope** | Módulo 2 — CRUD de flota + panel de disponibilidad |

---

## Project Structure

> Este plan extiende la estructura de PLAN-01 y PLAN-02. Los modelos `Vehiculo` y `Conductor` ya fueron creados en PLAN-02 Phase 2. Aquí se construyen los endpoints y servicios de administración.

```
src/
├── main/
│   └── java/com/logistics/routes/
│       ├── model/
│       │   ├── Vehiculo.java              (ya existe — PLAN-02)
│       │   ├── Conductor.java             (ya existe — PLAN-02)
│       │   └── HistorialAsignacion.java   [NUEVO]
│       ├── repository/
│       │   ├── VehiculoRepository.java    (ya existe — se extiende)
│       │   ├── ConductorRepository.java   (ya existe — se extiende)
│       │   └── HistorialAsignacionRepository.java [NUEVO]
│       ├── service/
│       │   ├── VehiculoService.java       [NUEVO]
│       │   └── ConductorService.java      [NUEVO]
│       ├── controller/
│       │   ├── VehiculoController.java    [NUEVO]
│       │   └── ConductorController.java   [NUEVO]
│       └── dto/
│           ├── VehiculoRequest.java       [NUEVO]
│           ├── VehiculoResponse.java      [NUEVO]
│           ├── ConductorRequest.java      [NUEVO]
│           ├── AsignacionRequest.java     [NUEVO]
│           └── FlotaDisponibilidadResponse.java [NUEVO]
└── test/
    └── java/com/logistics/routes/
        ├── service/
        │   ├── VehiculoServiceTest.java   [NUEVO]
        │   └── ConductorServiceTest.java  [NUEVO]
        └── integration/
            └── FlotaIntegrationTest.java  [NUEVO]
```

**Structure Decision:** Se extiende el mismo proyecto Spring Boot. Las entidades `Vehiculo` y `Conductor` ya existen en PLAN-02; aquí se agregan los servicios y endpoints de administración completos.

---

## Phase 1: Setup

> **Dependencia:** PLAN-01 Phase 1 y PLAN-02 Phase 2 (entidades `Vehiculo` y `Conductor`) deben estar completas.

- [ ] T001 Verificar que `Vehiculo`, `Conductor`, `EstadoVehiculo`, `EstadoConductor` compilan y tienen tablas en PostgreSQL

---

## Phase 2: Foundational — Historial de Asignaciones

**Purpose:** Entidad `HistorialAsignacion` necesaria para auditoría de asignaciones conductor-vehículo (FR-012). Bloquea las historias de asignación de conductores.

- [ ] T002 [P] Crear entidad `HistorialAsignacion` en `model/HistorialAsignacion.java`: `id` (UUID), `conductorId` (UUID), `vehiculoId` (UUID), `fechaHoraInicio` (LocalDateTime), `fechaHoraFin` (LocalDateTime, nullable)
- [ ] T003 [P] Crear `HistorialAsignacionRepository` con query: `findByConductorIdAndFechaHoraFinIsNull(UUID conductorId)` para verificar asignación activa
- [ ] T004 [P] Crear DTOs: `VehiculoRequest`, `VehiculoResponse`, `ConductorRequest`, `AsignacionRequest`, `FlotaDisponibilidadResponse`

**Checkpoint: Historial de asignaciones listo. Se puede iniciar implementación de las historias de flota.**

---

## Phase 3: User Story 3 — Gestión de Vehículos (Priority: P1)

**Goal:** El Administrador puede registrar, actualizar y dar de baja vehículos. El sistema valida placa única y bloquea cambios en vehículos con rutas activas.

**Independent Test:** Registrar un vehículo nuevo, verificar que aparece en `GET /api/vehiculos` con estado `DISPONIBLE`. Intentar registrar con la misma placa → error. Intentar dar de baja un vehículo en estado `EN_TRANSITO` → error.

### Tests para US3
- [ ] T005 [P] [US3] Test unitario: `VehiculoServiceTest` — registro exitoso de vehículo nuevo
- [ ] T006 [P] [US3] Test unitario: registro bloqueado por placa duplicada
- [ ] T007 [P] [US3] Test unitario: actualización bloqueada si vehículo está `EN_TRANSITO`
- [ ] T008 [P] [US3] Test unitario: baja exitosa de vehículo `DISPONIBLE` sin rutas activas
- [ ] T009 [US3] Test unitario: baja bloqueada de vehículo con estado `EN_TRANSITO`
- [ ] T010 [US3] Test unitario: recálculo de capacidad alerta al Despachador si hay sobrepeso tras actualizar capacidad
- [ ] T011 [US3] Test de integración: `FlotaIntegrationTest` — CRUD completo de vehículo

### Implementación para US3
- [ ] T012 [P] [US3] Implementar `VehiculoService.registrar(VehiculoRequest)`:
  - Validar que no exista vehículo con la misma placa → lanzar excepción con mensaje
  - Validar que `capacidadPesoKg > 0` → lanzar excepción con mensaje
  - Crear vehículo con estado `DISPONIBLE`
  - Retornar `VehiculoResponse` con `id` generado
- [ ] T013 [US3] Implementar `VehiculoService.actualizar(UUID id, VehiculoRequest)`:
  - Verificar que el vehículo no esté `EN_TRANSITO` → bloquear con mensaje
  - Actualizar atributos; si cambia `capacidadPesoKg`, recalcular rutas pendientes → notificar Despachador si sobrepeso
- [ ] T014 [US3] Implementar `VehiculoService.darDeBaja(UUID id)`:
  - Verificar estado no `EN_TRANSITO` y sin rutas en `LISTA_PARA_DESPACHO` o `CONFIRMADA` → bloquear con mensaje
  - Cambiar estado a `INACTIVO`
- [ ] T015 [US3] Implementar `VehiculoController` con endpoints:
  - `POST /api/vehiculos` — registrar
  - `GET /api/vehiculos` — listar todos
  - `GET /api/vehiculos/{id}` — detalle
  - `PUT /api/vehiculos/{id}` — actualizar
  - `DELETE /api/vehiculos/{id}` — dar de baja

**Checkpoint: CRUD de vehículos funcional con todas las validaciones.**

---

## Phase 4: User Story 4 — Asignación de Conductores a Vehículos (Priority: P2)

**Goal:** El Administrador asigna y desvincula conductores a vehículos. El sistema impide que un conductor esté en más de un vehículo activo y registra historial con fecha y hora.

**Independent Test:** Registrar conductor y vehículo, asignar, verificar que ambos se actualizan. Intentar asignar el mismo conductor a un segundo vehículo → error. Intentar reasignar conductor en vehículo `EN_TRANSITO` → error.

### Tests para US4
- [ ] T016 [P] [US4] Test unitario: `ConductorServiceTest` — asignación exitosa conductor-vehículo
- [ ] T017 [P] [US4] Test unitario: asignación bloqueada si conductor ya tiene vehículo activo
- [ ] T018 [P] [US4] Test unitario: reasignación bloqueada si vehículo está `EN_TRANSITO`
- [ ] T019 [US4] Test unitario: desvinculación exitosa registra `fechaHoraFin` en historial
- [ ] T020 [US4] Test unitario: alerta de alta prioridad al Despachador si conductor en `EN_RUTA` cambia a `INACTIVO`
- [ ] T021 [US4] Test de integración: flujo completo asignación/desvinculación con historial

### Implementación para US4
- [ ] T022 [P] [US4] Implementar `ConductorService.registrar(ConductorRequest)` — crear conductor con estado `ACTIVO`
- [ ] T023 [P] [US4] Implementar `ConductorService.asignar(UUID conductorId, AsignacionRequest vehiculoId)`:
  - Verificar conductor en estado `ACTIVO` y sin vehículo activo → error si ya tiene
  - Verificar vehículo en estado `DISPONIBLE` y no `EN_TRANSITO` → error si bloqueado
  - Crear registro `HistorialAsignacion` con `fechaHoraInicio = now()`
  - Actualizar `conductorId` en `Vehiculo` y `vehiculoAsignadoId` en `Conductor`
- [ ] T024 [US4] Implementar `ConductorService.desvincular(UUID conductorId)`:
  - Verificar que el vehículo asociado no esté `EN_TRANSITO` → bloquear con mensaje
  - Cerrar registro `HistorialAsignacion` con `fechaHoraFin = now()`
  - Limpiar `conductorId` del vehículo → vehículo queda excluido del algoritmo de planificación
- [ ] T025 [US4] Implementar `ConductorService.darDeBaja(UUID conductorId)`:
  - Si conductor está `EN_RUTA`: enviar alerta de alta prioridad al Despachador
  - Cambiar estado a `INACTIVO`
- [ ] T026 [US4] Implementar `ConductorController` con endpoints:
  - `POST /api/conductores` — registrar
  - `GET /api/conductores` — listar
  - `POST /api/conductores/{id}/asignar` — asignar vehículo
  - `DELETE /api/conductores/{id}/desvincular` — quitar vehículo
  - `GET /api/conductores/{id}/historial` — consultar historial de asignaciones

**Checkpoint: Asignaciones conductor-vehículo funcionan correctamente con historial y validaciones.**

---

## Phase 5: User Story 5 — Panel de Disponibilidad de Flota (Priority: P2)

**Goal:** El Administrador consulta en tiempo real el estado de todos los vehículos, diferenciando los disponibles con y sin conductor.

**Independent Test:** Llamar `GET /api/flota/disponibilidad` y verificar que cada vehículo muestra su estado, conductor asignado (o null), zona de operación y se diferencian visualmente los disponibles sin conductor.

### Tests para US5
- [ ] T027 [US5] Test unitario: panel retorna todos los vehículos con su estado y conductor
- [ ] T028 [US5] Test unitario: vehículo `DISPONIBLE` sin conductor se diferencia en la respuesta (`conductorAsignado: null`)
- [ ] T029 [US5] Test unitario: panel muestra estado `DISPONIBLE` / `EN_TRANSITO` / `INACTIVO` correctamente

### Implementación para US5
- [ ] T030 [US5] Implementar `VehiculoService.consultarDisponibilidad()` — retorna lista de `FlotaDisponibilidadResponse` con: `vehiculoId`, `placa`, `tipo`, `estado`, `zonaOperacion`, `conductorAsignado` (null si no tiene), `disponibleParaPlanificacion` (true si estado=`DISPONIBLE` y tiene conductor)
- [ ] T031 [US5] Implementar `VehiculoController.disponibilidad()` — `GET /api/flota/disponibilidad`

**Checkpoint: El Administrador puede ver la flota completa en tiempo real desde un solo endpoint.**

---

## Phase N: Polish y Concerns Transversales

- [ ] T032 Documentar todos los endpoints con Javadoc
- [ ] T033 Revisar logs estructurados para auditoría de cambios de estado de flota
- [ ] T034 Agregar validaciones `@Valid` en todos los DTOs de request
- [ ] T035 Revisar respuestas HTTP: 201 en creación, 404 en no encontrado, 409 en conflictos de negocio, 422 en validaciones

---

## Dependencies & Execution Order

```
PLAN-01 Phase 1 + Phase 2 (modelos Ruta, TipoVehiculo)
PLAN-02 Phase 2 (entidades Vehiculo, Conductor)
    └── Este plan — Phase 1 (verificación)
            └── Phase 2 (Foundational HistorialAsignacion — BLOQUEA US4)
                    ├── Phase 3 (US3: CRUD Vehículos) — P1, independiente
                    ├── Phase 4 (US4: Asignación Conductores) — P2, depende de Phase 3
                    └── Phase 5 (US5: Panel Disponibilidad) — P2, depende de Phase 3 y 4

Phase N (Polish) — al final
```

---

## Notes

- US3 y US4 comparten entidades pero sus servicios son independientes y pueden ir en paralelo si hay dos developers disponibles.
- El campo `disponibleParaPlanificacion` en `FlotaDisponibilidadResponse` es calculado (no almacenado): `estado == DISPONIBLE && conductorId != null`.
- La alerta al Despachador cuando un conductor pasa a `INACTIVO` estando `EN_RUTA` es un stub de `NotificacionService` aquí; se conectará al canal real más adelante.
- Verificar tests y hacer commit al terminar cada tarea o grupo lógico.
