# Implementation Plan: Gestión de Vehículos y Conductores (MOD2-UC-003 / MOD2-UC-004 / MOD2-UC-005)

**Date:** 2026-04-09 (actualizado)
**Specs:**
- [SPEC-03-gestion-vehiculos.md](../specs/SPEC-03-gestion-vehiculos.md)
- [SPEC-04-asignacion-conductores.md](../specs/SPEC-04-asignacion-conductores.md)
- [SPEC-05-disponibilidad-flota.md](../specs/SPEC-05-disponibilidad-flota.md)

**Arquitectura:** Ver [PLAN-00-arquitectura-master.md](./PLAN-00-arquitectura-master.md)
**Orden de ejecución:** Sprint 1 — depende solo de PLAN-00 (Sprint 0). Es **prerequisito** de PLAN-01 y PLAN-02.

---

## Summary

Este plan agrupa tres SPECs que comparten las mismas entidades (`Vehiculo`, `Conductor`) y son gestionadas por el mismo actor (Administrador de Flota). Cubre: registro, actualización y baja de vehículos (placa única, bloqueo si en tránsito); asignación y desvinculación de conductores con historial de asignaciones; y consulta en tiempo real del panel de disponibilidad.

> **Cambio respecto al plan original:** La estructura de paquetes es hexagonal (ver PLAN-00). `Vehiculo` y `Conductor` son entidades de dominio puras (POJO) además de tener sus entidades JPA correspondientes. Los servicios implementan puertos de entrada.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 21 |
| **Framework** | Spring Boot 3.x |
| **Arquitectura** | Hexagonal (Ports & Adapters) — ver PLAN-00 |
| **Primary Dependencies** | Spring Web, Spring Data JPA, Spring Security, PostgreSQL, Lombok |
| **Storage** | PostgreSQL |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Performance Goals** | Vehículo registrado visible al algoritmo en < 2 min. Panel actualizado en tiempo real. |
| **Constraints** | Placa única. Bloqueo de modificación/baja en vehículos con rutas activas. Conductor no puede estar en más de un vehículo activo. |

---

## Project Structure

```
domain/
├── model/
│   ├── Vehiculo.java                       [existente — PLAN-00] ← añadir modelo_contrato a Conductor
│   ├── Conductor.java                      [existente — PLAN-00]
│   └── HistorialAsignacion.java            [NUEVO en dominio]
├── enums/
│   ├── EstadoVehiculo.java                 [existente — PLAN-00]
│   └── EstadoConductor.java                [existente — PLAN-00]
├── exception/
│   ├── PlacaDuplicadaException.java        [existente — PLAN-00]
│   ├── VehiculoEnTransitoException.java    [existente — PLAN-00]
│   └── ConductorYaAsignadoException.java   [existente — PLAN-00]
└── port/
    ├── in/
    │   ├── GestionFlotaUseCase.java         [existente — PLAN-00]
    │   └── GestionConductoresUseCase.java   [existente — PLAN-00]
    └── out/
        ├── VehiculoRepositoryPort.java      [existente — PLAN-00]
        ├── ConductorRepositoryPort.java     [existente — PLAN-00]
        ├── HistorialAsignacionRepositoryPort.java [NUEVO]
        └── NotificacionDespachadorPort.java [existente — PLAN-00]

application/
└── flota/
    ├── VehiculoService.java                [NUEVO — implementa GestionFlotaUseCase]
    └── ConductorService.java               [NUEVO — implementa GestionConductoresUseCase]

infrastructure/
├── adapter/
│   ├── in/web/
│   │   ├── VehiculoController.java         [NUEVO]
│   │   └── ConductorController.java        [NUEVO — solo gestión admin de flota]
│   └── out/persistence/
│       ├── VehiculoJpaAdapter.java         [existente — PLAN-00, se extiende]
│       ├── ConductorJpaAdapter.java        [existente — PLAN-00, se extiende]
│       └── HistorialAsignacionJpaAdapter.java [NUEVO]
├── persistence/
│   ├── entity/
│   │   └── HistorialAsignacionEntity.java  [NUEVO]
│   └── repository/
│       └── HistorialAsignacionJpaRepository.java [NUEVO]
└── dto/
    ├── request/
    │   ├── VehiculoRequest.java            [NUEVO]
    │   ├── ConductorRequest.java           [NUEVO]
    │   └── AsignacionRequest.java          [NUEVO]
    └── response/
        ├── VehiculoResponse.java           [NUEVO]
        └── FlotaDisponibilidadResponse.java [NUEVO]
```

> **Nota:** `ConductorController` aquí solo cubre la gestión ADMIN (CRUD, asignación de vehículo). Las operaciones de campo del conductor (consultar ruta, iniciar tránsito, registrar parada) están en `ConductorOperacionController` — definido en PLAN-04.

---

## Phase 1: Prerequisitos

- [ ] T301 Verificar que `VehiculoEntity`, `ConductorEntity`, `VehiculoJpaAdapter`, `ConductorJpaAdapter` compilan desde PLAN-00
- [ ] T302 Verificar que `EstadoVehiculo`, `EstadoConductor`, `TipoVehiculo` están en `domain/enums/`

---

## Phase 2: Dominio — Historial de Asignaciones y modelo_contrato

**Purpose:** Completar el modelo de dominio con los atributos faltantes identificados en el diagnóstico.

- [ ] T303 [P] Agregar `modeloContrato` a la entidad `Conductor` en `domain/model/Conductor.java`:
  ```java
  // Enum en domain/enums/ModeloContrato.java
  public enum ModeloContrato {
      RECORRIDO_COMPLETO, POR_PARADA
  }
  // En Conductor: campo modeloContrato (ModeloContrato) — requerido por SPEC-08 payload RUTA_CERRADA
  ```
  
  > **¿Por qué aquí?** SPEC-08 (sección 4) requiere `modelo_contrato` en el evento `RUTA_CERRADA`. Sin este campo, el Módulo 3 no puede calcular la liquidación.

- [ ] T304 [P] Agregar `modeloContrato` a `ConductorEntity` y a la migración Flyway `V3__add_modelo_contrato.sql`:
  ```sql
  CREATE TYPE modelo_contrato AS ENUM ('RECORRIDO_COMPLETO', 'POR_PARADA');
  ALTER TABLE conductores ADD COLUMN modelo_contrato modelo_contrato NOT NULL DEFAULT 'POR_PARADA';
  ```

- [ ] T305 [P] Crear entidad de dominio `HistorialAsignacion` en `domain/model/HistorialAsignacion.java`:
  ```java
  public class HistorialAsignacion {
      private UUID id;
      private UUID conductorId;
      private UUID vehiculoId;
      private Instant fechaHoraInicio;
      private Instant fechaHoraFin; // null = asignación activa
  }
  ```

- [ ] T306 [P] Crear `HistorialAsignacionRepositoryPort`:
  ```java
  interface HistorialAsignacionRepositoryPort {
      void registrarInicio(UUID conductorId, UUID vehiculoId);
      void registrarFin(UUID conductorId);
      Optional<HistorialAsignacion> buscarAsignacionActiva(UUID conductorId);
      List<HistorialAsignacion> buscarHistorialPorConductor(UUID conductorId);
  }
  ```

- [ ] T307 [P] Crear `HistorialAsignacionEntity`, `HistorialAsignacionJpaRepository` e implementar `HistorialAsignacionJpaAdapter`

**Checkpoint: Dominio de flota completo incluyendo `modelo_contrato` en Conductor e historial de asignaciones.**

---

## Phase 3: User Story 3 — Gestión de Vehículos (Priority: P1)

**Goal:** Administrador puede registrar, actualizar y dar de baja vehículos con validaciones.

**Independent Test:** `POST /api/vehiculos` con datos válidos → 201 con `id`. Segundo `POST` con misma placa → 409. `DELETE /api/vehiculos/{id}` de vehículo `EN_TRANSITO` → 409.

### Tests (TDD)

- [ ] T308 [P] [US3] `VehiculoServiceTest` — registro exitoso:
  - Dado: `VehiculoRequest` con placa nueva y `capacidadPesoKg > 0`
  - Cuando: `VehiculoService.registrar(command)`
  - Entonces: `VehiculoRepositoryPort.guardar()` llamado con estado `DISPONIBLE`. Retorna `VehiculoResponse` con UUID.

- [ ] T309 [P] [US3] `VehiculoServiceTest` — registro bloqueado por placa duplicada:
  - `VehiculoRepositoryPort.existePorPlaca("ABC123")` → `true`
  - Entonces: lanza `PlacaDuplicadaException`. No llama a `guardar()`.

- [ ] T310 [P] [US3] `VehiculoServiceTest` — actualización bloqueada si `EN_TRANSITO`:
  - Vehículo con estado `EN_TRANSITO`
  - Entonces: lanza `VehiculoEnTransitoException`.

- [ ] T311 [P] [US3] `VehiculoServiceTest` — baja exitosa de vehículo `DISPONIBLE`:
  - Vehículo sin rutas activas → estado cambia a `INACTIVO`

- [ ] T312 [US3] `VehiculoServiceTest` — baja bloqueada si `EN_TRANSITO`

- [ ] T313 [US3] `VehiculoServiceTest` — capacidad ≤ 0 → excepción de validación

### Implementación

- [ ] T314 [P] [US3] Implementar `VehiculoService implements GestionFlotaUseCase`:

```java
@Service
@Transactional
@RequiredArgsConstructor
public class VehiculoService implements GestionFlotaUseCase {

    private final VehiculoRepositoryPort vehiculoRepository;
    private final NotificacionDespachadorPort notificacion;

    @Override
    public Vehiculo registrar(RegistrarVehiculoCommand command) {
        if (vehiculoRepository.existePorPlaca(command.placa())) {
            throw new PlacaDuplicadaException(command.placa());
        }
        Vehiculo vehiculo = Vehiculo.nuevo(command); // factory method en la entidad de dominio
        return vehiculoRepository.guardar(vehiculo);
    }

    @Override
    public void darDeBaja(UUID id) {
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(id)
            .orElseThrow(() -> new RutaNoEncontradaException(id));
        if (vehiculo.estado() == EstadoVehiculo.EN_TRANSITO) {
            throw new VehiculoEnTransitoException(id);
        }
        vehiculo.marcarInactivo();
        vehiculoRepository.guardar(vehiculo);
    }
    // ...
}
```

- [ ] T315 [US3] Extender `VehiculoRepositoryPort` con:
  - `boolean existePorPlaca(String placa)`
  - `List<Vehiculo> buscarTodos()`

- [ ] T316 [US3] Implementar `VehiculoController`:

```java
@RestController
@RequestMapping("/api/vehiculos")
@PreAuthorize("hasRole('FLEET_ADMIN')")
@RequiredArgsConstructor
public class VehiculoController {

    private final GestionFlotaUseCase flota; // ← depende del puerto, no del servicio

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse registrar(@Valid @RequestBody VehiculoRequest request) { ... }

    @GetMapping
    public List<VehiculoResponse> listar() { ... }

    @GetMapping("/{id}")
    public VehiculoResponse detalle(@PathVariable UUID id) { ... }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable UUID id,
                                       @Valid @RequestBody VehiculoRequest request) { ... }

    @DeleteMapping("/{id}")
    public void darDeBaja(@PathVariable UUID id) { ... }
}
```

**Checkpoint: CRUD de vehículos funcional con todas las validaciones. Vehículo registrado disponible para el algoritmo.**

---

## Phase 4: User Story 4 — Asignación de Conductores (Priority: P2)

**Goal:** Administrador asigna y desvincula conductores a vehículos con historial y validaciones.

**Independent Test:** `POST /api/conductores/{id}/asignar-vehiculo` con vehículo `DISPONIBLE` y conductor `ACTIVO` sin vehículo → 200. Segundo intento de asignar el mismo conductor → 409.

### Tests (TDD)

- [ ] T317 [P] [US4] `ConductorServiceTest` — asignación exitosa:
  - Dado: conductor `ACTIVO` sin vehículo, vehículo `DISPONIBLE`
  - Cuando: `ConductorService.asignarVehiculo(conductorId, vehiculoId)`
  - Entonces: `HistorialAsignacionRepositoryPort.registrarInicio()` llamado. `conductor.vehiculoAsignadoId` actualizado. `vehiculo.conductorId` actualizado.

- [ ] T318 [P] [US4] `ConductorServiceTest` — asignación bloqueada si conductor ya tiene vehículo:
  - `HistorialAsignacionRepositoryPort.buscarAsignacionActiva(conductorId)` retorna asignación existente
  - Entonces: lanza `ConductorYaAsignadoException`

- [ ] T319 [P] [US4] `ConductorServiceTest` — reasignación bloqueada si vehículo `EN_TRANSITO`

- [ ] T320 [US4] `ConductorServiceTest` — desvinculación exitosa:
  - Cuando: `ConductorService.desvincularVehiculo(conductorId)`
  - Entonces: `HistorialAsignacionRepositoryPort.registrarFin()` llamado. `vehiculo.conductorId` = null. Vehículo queda excluido del algoritmo de planificación (calculado: `disponibleParaPlanificacion = estado==DISPONIBLE && conductorId != null`).

- [ ] T321 [US4] `ConductorServiceTest` — baja de conductor `EN_RUTA` → alerta al Despachador:
  - `NotificacionDespachadorPort.notificarAlertaPrioritaria()` llamado antes de cambiar estado

### Implementación

- [ ] T322 [P] [US4] Implementar `ConductorService implements GestionConductoresUseCase`:

```java
@Service
@Transactional
@RequiredArgsConstructor
public class ConductorService implements GestionConductoresUseCase {

    private final ConductorRepositoryPort conductorRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final HistorialAsignacionRepositoryPort historialRepository;
    private final NotificacionDespachadorPort notificacion;

    @Override
    public void asignarVehiculo(UUID conductorId, UUID vehiculoId) {
        Conductor conductor = conductorRepository.buscarPorId(conductorId)
            .orElseThrow(() -> new RutaNoEncontradaException(conductorId));
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(vehiculoId)
            .orElseThrow(() -> new RutaNoEncontradaException(vehiculoId));

        historialRepository.buscarAsignacionActiva(conductorId)
            .ifPresent(a -> { throw new ConductorYaAsignadoException(conductorId); });

        if (vehiculo.estado() == EstadoVehiculo.EN_TRANSITO) {
            throw new VehiculoEnTransitoException(vehiculoId);
        }

        conductor.asignarVehiculo(vehiculoId);
        vehiculo.asignarConductor(conductorId);
        historialRepository.registrarInicio(conductorId, vehiculoId);

        conductorRepository.guardar(conductor);
        vehiculoRepository.guardar(vehiculo);
    }

    @Override
    public void darDeBaja(UUID conductorId) {
        Conductor conductor = conductorRepository.buscarPorId(conductorId)
            .orElseThrow(() -> new RutaNoEncontradaException(conductorId));

        if (conductor.estado() == EstadoConductor.EN_RUTA) {
            notificacion.notificarAlertaPrioritaria(
                "Conductor " + conductor.nombre() + " dado de baja mientras está EN_RUTA"
            );
        }
        conductor.marcarInactivo();
        conductorRepository.guardar(conductor);
    }
    // ...
}
```

- [ ] T323 [US4] Implementar `ConductorController` (gestión admin — no incluir endpoints de operación de campo):

```java
@RestController
@RequestMapping("/api/conductores")
@PreAuthorize("hasRole('FLEET_ADMIN')")
@RequiredArgsConstructor
public class ConductorController {

    private final GestionConductoresUseCase conductores;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse registrar(@Valid @RequestBody ConductorRequest request) { ... }

    @GetMapping
    public List<ConductorResponse> listar() { ... }

    @PostMapping("/{id}/asignar-vehiculo")
    public void asignarVehiculo(@PathVariable UUID id,
                                @Valid @RequestBody AsignacionRequest request) { ... }

    @DeleteMapping("/{id}/desvincular-vehiculo")
    public void desvincularVehiculo(@PathVariable UUID id) { ... }

    @PutMapping("/{id}/estado")
    public void cambiarEstado(@PathVariable UUID id, @RequestParam EstadoConductor estado) { ... }

    @GetMapping("/{id}/historial-asignaciones")
    public List<HistorialAsignacionResponse> historial(@PathVariable UUID id) { ... }
}
```

**Checkpoint: Asignaciones conductor-vehículo funcionan con historial y validaciones.**

---

## Phase 5: User Story 5 — Panel de Disponibilidad (Priority: P2)

- [ ] T324 [US5] `VehiculoServiceTest` — `consultarDisponibilidad()` retorna `disponibleParaPlanificacion = true` solo si estado `DISPONIBLE` Y `conductorId != null`

- [ ] T325 [US5] Implementar `VehiculoService.consultarDisponibilidad()`:
  ```java
  // Campo calculado — no almacenado en BD
  boolean disponibleParaPlanificacion = vehiculo.estado() == EstadoVehiculo.DISPONIBLE
      && vehiculo.conductorId() != null;
  ```

- [ ] T326 [US5] Agregar endpoint en `VehiculoController`:
  ```java
  @GetMapping("/disponibilidad")    // accesible también para ROLE_DISPATCHER
  @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
  public List<FlotaDisponibilidadResponse> disponibilidad() { ... }
  ```

**Checkpoint: Admin y Despachador pueden ver la flota completa y distinguir vehículos disponibles sin conductor.**

---

## Phase N: Polish

- [ ] T327 Documentar todos los endpoints con `@Operation`, `@ApiResponse` de OpenAPI
- [ ] T328 Validaciones `@Valid` en `VehiculoRequest` (placa no nula, capacidad > 0) y `ConductorRequest`
- [ ] T329 Respuestas HTTP: 201 en creación, 404 en no encontrado, 409 en conflictos de negocio, 422 en validaciones
- [ ] T330 Test de integración `FlotaIntegrationTest` con Testcontainers: CRUD completo de vehículo + asignación/desvinculación de conductor con historial

---

## Dependencies & Execution Order

```
PLAN-00 Sprint 0 (Fundación hexagonal — entidades JPA de Vehiculo y Conductor ya estarán)
    └── Este plan — Sprint 1 (primer sprint de negocio — prerequisito de los demás)
            ├── Phase 2 (Historial + modelo_contrato — BLOQUEA US4)
            ├── Phase 3 (US3: CRUD Vehículos) ← P1, comenzar aquí
            ├── Phase 4 (US4: Asignación Conductores) ← P2
            └── Phase 5 (US5: Panel Disponibilidad) ← P2
```

---

## Notes

- Ejecutar este plan **antes** de PLAN-01. Sin vehículos en BD, el algoritmo de consolidación no puede asignar `tipoVehiculoRequerido`.  
- `modelo_contrato` es el campo que faltaba en los planes originales. Es crítico para SPEC-08 (payload `RUTA_CERRADA` al Módulo 3).
- `ConductorController` aquí es **solo gestión admin** (PLAN-03). Las operaciones de campo del conductor están en `ConductorOperacionController` en PLAN-04 — esto resuelve el conflicto de nombres del diseño anterior.
- El campo `disponibleParaPlanificacion` en `FlotaDisponibilidadResponse` es calculado en tiempo de query, no almacenado en BD.
