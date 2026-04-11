# Implementation Plan: Despacho de Rutas (MOD2-UC-002)

**Date:** 2026-04-09 (actualizado)
**Spec:** [SPEC-02-despacho-rutas.md](../specs/SPEC-02-despacho-rutas.md)
**Arquitectura:** Ver [PLAN-00-arquitectura-master.md](./PLAN-00-arquitectura-master.md)
**Orden de ejecución:** Sprint 3 — depende de PLAN-00 (Sprint 0), PLAN-03 (Sprint 1) y PLAN-01 (Sprint 2)

---

## Summary

El Despachador Logístico revisa las rutas en estado `LISTA_PARA_DESPACHO`, asigna conductor y vehículo físico disponibles, optimiza el orden de paradas (algoritmo nearest-neighbor), genera el manifiesto y confirma el despacho. La ruta pasa a `CONFIRMADA` y el conductor recibe su ruta en el dispositivo. El sistema bloquea la confirmación si no hay conductor `ACTIVO` o vehículo `DISPONIBLE` del tipo requerido.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 21 |
| **Framework** | Spring Boot 3.x |
| **Arquitectura** | Hexagonal (Ports & Adapters) — ver PLAN-00 |
| **Primary Dependencies** | Spring Web, Spring Data JPA, Spring Security, Spring WebSocket (notificación conductor) |
| **Storage** | PostgreSQL |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Performance Goals** | Ruta `CONFIRMADA` y conductor notificado en < 5 seg |
| **Constraints** | Bloquear confirmación si no hay conductor `ACTIVO` o vehículo `DISPONIBLE`. Ruta en `LISTA_PARA_DESPACHO` no acepta nuevos paquetes. |

---

## Project Structure

```
domain/
├── model/
│   ├── Ruta.java                            [existente — PLAN-00]
│   ├── Vehiculo.java                        [existente — PLAN-00]
│   ├── Conductor.java                       [existente — PLAN-00]
│   └── Parada.java                          [existente — PLAN-00]
└── exception/
    ├── RutaNoEncontradaException.java       [existente — PLAN-00]
    ├── VehiculoNoDisponibleException.java   [existente — PLAN-00]
    └── ConductorNoDisponibleException.java  [existente — PLAN-00]

application/
├── port/
│   └── in/
│       ├── ListarRutasParaDespachoPort.java [existente — PLAN-00]
│       ├── ConfirmarDespachoPort.java       [existente — PLAN-00]
│       └── ExcluirPaqueteRutaPort.java      [existente — PLAN-00]
└── usecase/
    ├── ListarRutasParaDespachoUseCase.java  [NUEVO — implements ListarRutasParaDespachoPort]
    ├── ConfirmarDespachoUseCase.java        [NUEVO — implements ConfirmarDespachoPort]
    └── ExcluirPaqueteRutaUseCase.java       [NUEVO — implements ExcluirPaqueteRutaPort]

infrastructure/
├── adapter/
│   └── in/
│       └── web/
│           └── DespachoController.java     [NUEVO — inyecta los 3 puertos individuales]
└── dto/
    ├── request/
    │   └── ConfirmarDespachoRequest.java   [NUEVO]
    └── response/
        └── RutaDetalleResponse.java        [NUEVO]
```

---

## Phase 1: Prerequisitos (verificación)

> **Dependencia bloqueante:** PLAN-01 Sprint 2 completo. Debe haber rutas en estado `LISTA_PARA_DESPACHO`.

- [ ] T201 Verificar que existen rutas en estado `LISTA_PARA_DESPACHO` en BD (PLAN-01 completo)
- [ ] T202 Verificar que `VehiculoRepositoryPort` y `ConductorRepositoryPort` tienen métodos para buscar por estado (PLAN-03 completo)
- [ ] T203 Verificar que `ListarRutasParaDespachoPort`, `ConfirmarDespachoPort` y `ExcluirPaqueteRutaPort` están definidos en `application/port/in/` (PLAN-00 completo)

---

## Phase 2: Puertos de salida — extensión de repositorios

- [ ] T204 [P] Extender `VehiculoRepositoryPort` con:
  ```java
  Optional<Vehiculo> buscarDisponiblePorTipo(TipoVehiculo tipo);
  ```
- [ ] T205 [P] Extender `ConductorRepositoryPort` con:
  ```java
  Optional<Conductor> buscarActivo();
  // O mejor: que el comando ConfirmarDespacho traiga conductorId y vehiculoId explícitos del Despachador
  ```
- [ ] T206 [P] Extender `RutaRepositoryPort` con:
  ```java
  List<Ruta> buscarPorEstado(EstadoRuta estado);
  void excluirPaquete(UUID rutaId, UUID paqueteId);
  ```

---

## Phase 3: DespachoService — listar y confirmar despacho (US2)

**Goal:** El Despachador puede ver rutas listas y confirmar el despacho con conductor y vehículo asignados.

**Independent Test:** Instanciar `DespachoService` con mocks de puertos. Tener una ruta `LISTA_PARA_DESPACHO` con conductor mock `ACTIVO` y vehículo mock `DISPONIBLE`. Llamar `confirmarDespacho()` → verificar que la ruta queda `CONFIRMADA` y los estados de conductor y vehículo se actualizan.

### Tests (TDD)

- [ ] T207 [P] [US2] `ListarRutasParaDespachoUseCaseTest` — listar rutas para despacho:
  - `ListarRutasParaDespachoUseCase.ejecutar()` delega a `RutaRepositoryPort.buscarPorEstado(LISTA_PARA_DESPACHO)`
  - Lista vacía cuando no hay rutas en ese estado

- [ ] T208 [P] [US2] `ConfirmarDespachoUseCaseTest` — confirmación exitosa:
  - Dado: ruta `LISTA_PARA_DESPACHO`, conductor `ACTIVO` y vehículo `DISPONIBLE` del tipo requerido
  - Cuando: `ConfirmarDespachoUseCase.ejecutar(rutaId, command)` con `conductorId` y `vehiculoId` en el command
  - Entonces: ruta `CONFIRMADA`, vehículo → `EN_TRANSITO`, conductor → `EN_RUTA`

- [ ] T209 [P] [US2] `ConfirmarDespachoUseCaseTest` — bloqueado si conductor no `ACTIVO`:
  - Dado: conductorId referencia a conductor con estado `INACTIVO`
  - Entonces: lanza `ConductorNoDisponibleException`. Ruta no cambia de estado.

- [ ] T210 [P] [US2] `ConfirmarDespachoUseCaseTest` — bloqueado si vehículo no `DISPONIBLE`:
  - Dado: vehiculoId referencia a vehículo con estado `EN_TRANSITO`
  - Entonces: lanza `VehiculoNoDisponibleException`. Ruta no cambia de estado.

- [ ] T211 [US2] `ExcluirPaqueteRutaUseCaseTest` — exclusión de paquete antes de confirmar:
  - `ExcluirPaqueteRutaUseCase.ejecutar(rutaId, paqueteId, motivo)` → parada queda marcada con estado `EXCLUIDA_DESPACHO`. `IntegracionModulo1Port.publishPaqueteExcluidoDespacho()` llamado. Ruta sigue en `LISTA_PARA_DESPACHO`.

- [ ] T212 [US2] `ConfirmarDespachoUseCaseTest` — optimización de paradas:
  - `optimizarOrdenParadas(paradas)` ordena la lista por nearest-neighbor desde el punto de origen. Verifica que el orden resultante tiene menor distancia total que el orden original.

### Implementación

- [ ] T213 [P] [US2] Implementar `ConfirmarDespachoUseCase implements ConfirmarDespachoPort`:

```java
// application/usecase/ConfirmarDespachoUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmarDespachoUseCase implements ConfirmarDespachoPort {

    private final RutaRepositoryPort rutaRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final ConductorRepositoryPort conductorRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionM1;

    @Override
    public Ruta ejecutar(UUID rutaId, ConfirmarDespachoCommand command) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
            .orElseThrow(() -> new RutaNoEncontradaException(rutaId));

        Conductor conductor = conductorRepository.buscarPorId(command.conductorId())
            .filter(c -> c.estado() == EstadoConductor.ACTIVO)
            .orElseThrow(() -> new ConductorNoDisponibleException(command.conductorId()));

        Vehiculo vehiculo = vehiculoRepository.buscarPorId(command.vehiculoId())
            .filter(v -> v.estado() == EstadoVehiculo.DISPONIBLE)
            .filter(v -> v.tipo() == ruta.tipoVehiculoRequerido())
            .orElseThrow(() -> new VehiculoNoDisponibleException(command.vehiculoId()));

        List<Parada> paradas = paradaRepository.buscarPorRutaId(rutaId);
        List<Parada> paradasOrdenadas = optimizarOrdenParadas(paradas);
        paradaRepository.guardarTodas(paradasOrdenadas);

        ruta.confirmar(conductor.id(), vehiculo.id());
        vehiculo.marcarEnTransito();
        conductor.marcarEnRuta();

        rutaRepository.guardar(ruta);
        vehiculoRepository.guardar(vehiculo);
        conductorRepository.guardar(conductor);

        return ruta;
    }

    private List<Parada> optimizarOrdenParadas(List<Parada> paradas) {
        // Algoritmo nearest-neighbor: O(n²) — suficiente para n < 50 paradas por ruta
        // Punto de inicio: centroide de la zona o primera parada
        // ...
    }
}
```

- [ ] T213b [US2] Implementar `ListarRutasParaDespachoUseCase implements ListarRutasParaDespachoPort`:

```java
// application/usecase/ListarRutasParaDespachoUseCase.java
@Service
@RequiredArgsConstructor
public class ListarRutasParaDespachoUseCase implements ListarRutasParaDespachoPort {

    private final RutaRepositoryPort rutaRepository;

    @Override
    public List<Ruta> ejecutar() {
        return rutaRepository.buscarPorEstado(EstadoRuta.LISTA_PARA_DESPACHO);
    }
}
```

- [ ] T213c [US2] Implementar `ExcluirPaqueteRutaUseCase implements ExcluirPaqueteRutaPort`:

```java
// application/usecase/ExcluirPaqueteRutaUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class ExcluirPaqueteRutaUseCase implements ExcluirPaqueteRutaPort {

    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionM1;

    @Override
    public void ejecutar(UUID rutaId, UUID paqueteId, String motivo) {
        Parada parada = paradaRepository.buscarPorRutaYPaquete(rutaId, paqueteId)
            .orElseThrow(() -> new ParadaNoEncontradaException(paqueteId));
        parada.marcarExcluidaDespacho(motivo);
        paradaRepository.guardar(parada);
        integracionM1.publishPaqueteExcluidoDespacho(paqueteId, rutaId, Instant.now());
    }
}
```

- [ ] T214 [US2] `ConfirmarDespachoCommand` record con `conductorId` y `vehiculoId` (el Despachador los elige explícitamente desde el UI)
- [ ] T215 [US2] Algoritmo nearest-neighbor en método privado `optimizarOrdenParadas(List<Parada> paradas)`:
  - Calcular distancia euclidiana entre coordenadas (Haversine para mayor precisión)
  - Iniciar desde la parada más cercana al centroide de la zona
  - Greedy: en cada paso tomar la parada no visitada más cercana a la actual
  - Asignar el campo `orden` a cada parada según el resultado

---

## Phase 4: Controller REST

- [ ] T216 [US2] Implementar `DespachoController`:

```java
@RestController
@RequestMapping("/api/despacho")
@PreAuthorize("hasRole('DISPATCHER')")
@RequiredArgsConstructor
public class DespachoController {

    // Cada campo inyecta el puerto individual que necesita — SRP en el controller
    private final ListarRutasParaDespachoPort listarRutas;
    private final ConfirmarDespachoPort confirmarDespacho;
    private final ExcluirPaqueteRutaPort excluirPaquete;
    private final ForzarCierreRutaPort forzarCierre;  // viene de PLAN-04

    @GetMapping("/rutas")
    public List<RutaDetalleResponse> listarRutasParaDespacho() {
        return listarRutas.ejecutar().stream().map(RutaDetalleResponse::from).toList();
    }

    @PostMapping("/rutas/{id}/confirmar")
    public RutaDetalleResponse confirmar(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmarDespachoRequest request) {
        Ruta ruta = confirmarDespacho.ejecutar(id, request.toCommand());
        return RutaDetalleResponse.from(ruta);
    }

    @DeleteMapping("/rutas/{id}/paquetes/{paqueteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @PathVariable UUID id,
            @PathVariable UUID paqueteId,
            @RequestParam String motivo) {
        excluirPaquete.ejecutar(id, paqueteId, motivo);
    }

    @PostMapping("/rutas/{id}/forzar-cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forzarCierre(@PathVariable UUID id) {
        forzarCierre.ejecutar(id);
    }
}
```

**Checkpoint: El Despachador puede listar rutas, confirmar despacho con conductor y vehículo, y excluir paquetes. Ruta queda `CONFIRMADA`.**

---

## Phase 5: DTOs

- [ ] T217 [US2] `ConfirmarDespachoRequest` record:
  ```java
  public record ConfirmarDespachoRequest(
      @NotNull UUID conductorId,
      @NotNull UUID vehiculoId
  ) {}
  ```

- [ ] T218 [US2] `RutaDetalleResponse` record con zona, cantidad de paquetes, peso acumulado, tipo de vehículo requerido, conductor asignado (nullable), vehículo asignado (nullable), lista de paradas ordenadas

---

## Phase N: Polish

- [ ] T219 Documentar endpoints con `@Operation`, `@ApiResponse` de OpenAPI
- [ ] T220 Validaciones `@Valid` en `ConfirmarDespachoRequest`
- [ ] T221 Respuestas HTTP correctas: 200 en confirmación, 404 si ruta no existe, 409 en conflictos de negocio, 422 en validaciones
- [ ] T222 Test de integración `DespachoIntegrationTest` con Testcontainers: flujo completo desde ruta `LISTA_PARA_DESPACHO` hasta `CONFIRMADA`

---

## Dependencies & Execution Order

```
PLAN-00 Sprint 0 (Fundación hexagonal)
    └── PLAN-03 Sprint 1 (Vehiculo, Conductor en BD)
            └── PLAN-01 Sprint 2 (rutas en LISTA_PARA_DESPACHO)
                    └── Este plan — Sprint 3
                            ├── Phase 2 (extensión de puertos)
                            ├── Phase 3 (DespachoService — TDD primero)
                            ├── Phase 4 (Controller REST)
                            └── Phase 5 (DTOs)
```

---

## Notes

- `optimizarOrdenParadas` usa nearest-neighbor básico (O(n²)). Suficiente para rutas de hasta ~50 paradas. Para optimización avanzada (TSP), considerar una librería como `optaplanner` en una iteración futura.
- El Despachador elige explícitamente conductor y vehículo desde el UI. El sistema valida que estén disponibles, pero no los elige automáticamente (esto evita asignaciones incorrectas y da control operativo al Despachador).
- La notificación al conductor (stub `NotificacionDespachadorPort`) se conectará a WebSocket en Sprint 5 (Frontend React). Hasta entonces, registrar el payload como log INFO.
