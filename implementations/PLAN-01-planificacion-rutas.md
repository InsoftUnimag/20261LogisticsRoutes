# Implementation Plan: Planificación Automática de Rutas (MOD2-UC-001)

**Date:** 2026-04-09 (actualizado)
**Spec:** [SPEC-01-planificacion-rutas.md](../specs/SPEC-01-planificacion-rutas.md)
**Arquitectura:** Ver [PLAN-00-arquitectura-master.md](./PLAN-00-arquitectura-master.md)
**Orden de ejecución:** Sprint 2 — depende de PLAN-00 (Sprint 0) y PLAN-03 (Sprint 1)

---

## Summary

El sistema recibe eventos `SOLICITAR_RUTA` del Módulo 1 (SGP) cuando un paquete alcanza estado `recibido_en_sede`. Agrupa paquetes por zona geográfica usando **geohash de precisión 5** (~4.9 km), asigna el tipo de vehículo de menor capacidad disponible, recalcula y escala el tipo de vehículo al superar el 90% de capacidad, transiciona rutas a `LISTA_PARA_DESPACHO` por capacidad o por vencimiento de plazo (5 días), y notifica al Despachador Logístico. La comunicación con Módulo 1 es asíncrona vía **SQS**.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 21 |
| **Framework** | Spring Boot 3.x |
| **Arquitectura** | Hexagonal (Ports & Adapters) — ver PLAN-00 |
| **Primary Dependencies** | Spring Web, Spring Data JPA, Spring Security, PostgreSQL, Flyway, geohash-java, ShedLock |
| **Storage** | PostgreSQL |
| **Mensajería** | Amazon SQS (Spring Cloud AWS) — reemplaza endpoint REST para producción |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Performance Goals** | Retornar `ruta_id` al SGP en < 2 seg; transición a `LISTA_PARA_DESPACHO` notificada en < 5 seg |
| **Constraints** | Una sola ruta `CREADA` por zona — garantizado por `UNIQUE INDEX PARTIAL` en BD, no por locks de aplicación |

---

## Project Structure

> Se usa la estructura hexagonal definida en PLAN-00. Los archivos de este plan identifican dónde va cada clase.

```
domain/
├── enums/
│   ├── EstadoRuta.java             [existente — PLAN-00]
│   └── TipoVehiculo.java           [existente — PLAN-00, con siguienteTipo() y capacidadKg()]
├── model/
│   └── Ruta.java                   [existente — PLAN-00]
├── valueobject/
│   └── ZonaGeografica.java         [existente — PLAN-00]
├── exception/
│   └── FechaLimiteVencidaException.java [existente — PLAN-00]
└── port/
    ├── in/
    │   └── PlanificacionRouteUseCase.java [existente — PLAN-00]
    └── out/
        ├── RutaRepositoryPort.java         [existente — PLAN-00]
        └── NotificacionDespachadorPort.java [existente — PLAN-00]

application/
└── planificacion/
    └── PlanificacionService.java   [NUEVO — implementa PlanificacionRouteUseCase]

infrastructure/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── PlanificacionController.java  [NUEVO]
│   │   └── messaging/
│   │       └── SolicitarRutaConsumer.java    [NUEVO — listener SQS]
│   └── out/
│       └── persistence/
│           └── RutaJpaAdapter.java           [existente — PLAN-00, se extiende]
├── scheduler/
│   └── FechaLimiteDespachoScheduler.java    [NUEVO]
└── dto/
    ├── request/
    │   └── SolicitarRutaRequest.java        [NUEVO]
    └── response/
        └── SolicitarRutaResponse.java       [NUEVO]
```

---

## Phase 1: Prerequisitos (verificación)

> **Dependencia bloqueante:** PLAN-00 Sprint 0 debe estar completo antes de comenzar.

- [ ] T101 Verificar que `ZonaGeografica`, `TipoVehiculo` (con `siguienteTipo()` y `capacidadKg()`), `EstadoRuta` compilan en `domain/`
- [ ] T102 Verificar que `RutaEntity` tiene el unique index parcial `idx_rutas_zona_creada` en BD (confirmar con `\d rutas` en psql)
- [ ] T103 Verificar que `RutaRepositoryPort` y `NotificacionDespachadorPort` están definidas en `application/port/out/`
- [ ] T104 Verificar que vehículos y conductores existen en BD (PLAN-03 Sprint 1 completo)

---

## Phase 2: Dominio de Planificación — Lógica del algoritmo de consolidación

**Purpose:** La lógica de negocio vive en el dominio y en el servicio de aplicación. Sin dependencias de Spring.

⚠️ **CRÍTICO: Nada del algoritmo de consolidación puede tener un import de Spring.**

### Tests del algoritmo (TDD puro)

- [ ] T105 [P] Test unitario `TipoVehiculoTest`:
  - `MOTO.siguienteTipo()` → `VAN`
  - `VAN.siguienteTipo()` → `NHR`
  - `NHR.siguienteTipo()` → `TURBO`
  - `TURBO.siguienteTipo()` → `Optional.empty()`
  - `MOTO.capacidadKg()` → `20.0`
  - `TURBO.capacidadKg()` → `4500.0`

- [ ] T106 [P] Test unitario `ZonaGeograficaTest`:
  - `ZonaGeografica.from(lat, lon)` retorna geohash de exactamente 5 caracteres
  - Dos coordenadas separadas < 5 km retornan el mismo geohash
  - Dos coordenadas separadas > 10 km retornan distinto geohash

### Implementación del dominio

- [ ] T107 [P] En `TipoVehiculo` enum: agregar método `Optional<TipoVehiculo> siguienteTipo()` y `double capacidadKg()`

```java
public enum TipoVehiculo {
    MOTO(20.0), VAN(500.0), NHR(2000.0), TURBO(4500.0);

    private final double capacidadKg;

    TipoVehiculo(double capacidadKg) { this.capacidadKg = capacidadKg; }

    public double capacidadKg() { return capacidadKg; }

    public Optional<TipoVehiculo> siguienteTipo() {
        TipoVehiculo[] values = values();
        int next = ordinal() + 1;
        return next < values.length ? Optional.of(values[next]) : Optional.empty();
    }

    public boolean porcentajeExcede(double pesoAcumulado, double umbral) {
        return (pesoAcumulado / capacidadKg) * 100 >= umbral;
    }
}
```

- [ ] T108 [P] En `ZonaGeografica` value object: usar `com.github.davidmoten:geo` o `ch.hsr:geohash`

```java
public record ZonaGeografica(String geohash) {
    private static final int PRECISION = 5;

    public static ZonaGeografica from(double latitud, double longitud) {
        return new ZonaGeografica(
            GeoHash.geoHashStringWithCharacterPrecision(latitud, longitud, PRECISION)
        );
    }
}
```

---

## Phase 3: Servicio de Aplicación — PlanificacionService (US1)

**Goal:** Recibir el comando `SolicitarRuta`, aplicar el algoritmo de consolidación y retornar `ruta_id`.

**Independent Test:** Instanciar `PlanificacionService` con mocks de los puertos de salida, enviar un `SolicitarRutaCommand` válido y verificar que retorna un UUID y que el puerto `RutaRepositoryPort.guardar()` fue llamado con los atributos correctos.

### Tests del servicio (TDD)

- [ ] T109 [P] [US1] `PlanificacionServiceTest` — paquete asignado a ruta `CREADA` existente en zona coincidente:
  - Dado: existe ruta `CREADA` con `zona = "d29ej"` y `pesoAcumulado = 5.0 kg` (tipo MOTO, capacidad 20 kg)
  - Cuando: llega paquete con `peso = 5.0 kg` y coordenadas que resultan en zona `"d29ej"`
  - Entonces: `RutaRepositoryPort.guardar()` con `pesoAcumulado = 10.0 kg`, retorna el mismo `ruta_id`

- [ ] T110 [P] [US1] `PlanificacionServiceTest` — sin ruta `CREADA` en zona → crea nueva ruta:
  - Dado: ninguna ruta `CREADA` para la zona calculada
  - Cuando: llega solicitud con `peso = 3.0 kg`, `latitud/longitud` del paquete
  - Entonces: `RutaRepositoryPort.guardar()` con estado `CREADA`, `tipoVehiculo = MOTO`, `fechaLimiteDespacho = now() + 5 días`

- [ ] T111 [P] [US1] `PlanificacionServiceTest` — escalar tipo de vehículo al superar 90%:
  - Dado: ruta `CREADA` con `pesoAcumulado = 17.0 kg`, tipo `MOTO` (capacidad 20 kg → 90% = 18 kg)
  - Cuando: llega paquete con `peso = 1.5 kg` → total `18.5 kg` → supera 90%
  - Entonces: `tipoVehiculoRequerido` escala a `VAN`. Ruta continúa en estado `CREADA`.

- [ ] T112 [P] [US1] `PlanificacionServiceTest` — ruta pasa a `LISTA_PARA_DESPACHO` al superar 90% de TURBO:
  - Dado: ruta `CREADA` con `pesoAcumulado = 4050.0 kg`, tipo `TURBO` (4500 kg → 90% = 4050 kg)
  - Cuando: llega paquete con `peso = 1.0 kg` → total `4051.0 kg` → supera 90%
  - Entonces: ruta transiciona a `LISTA_PARA_DESPACHO`. `NotificacionDespachadorPort.notificarRutaListaParaDespacho()` llamado.

- [ ] T113 [P] [US1] `PlanificacionServiceTest` — `fecha_limite_entrega` vencida → descarta solicitud:
  - Dado: paquete con `fechaLimiteEntrega` en el pasado
  - Entonces: lanza `FechaLimiteVencidaException`. No se crea ni modifica ninguna ruta. `NotificacionDespachadorPort.notificarAlertaPrioritaria()` llamado.

- [ ] T114 [US1] `PlanificacionServiceTest` — despacho manual anticipado:
  - Dado: ruta en estado `CREADA` con al menos un paquete
  - Cuando: `PlanificacionService.despacharManual(rutaId)`
  - Entonces: ruta transiciona a `LISTA_PARA_DESPACHO`

### Implementación del servicio

- [ ] T115 [P] [US1] Implementar `PlanificacionService implements PlanificacionRouteUseCase`:

```java
// application/planificacion/PlanificacionService.java
@Service
@Transactional
public class PlanificacionService implements PlanificacionRouteUseCase {

    private final RutaRepositoryPort rutaRepository;
    private final NotificacionDespachadorPort notificacion;
    private static final double UMBRAL_CAPACIDAD = 90.0;
    private static final int PLAZO_DESPACHO_DIAS = 5;

    // Inyección por constructor (no @Autowired en campo)
    public PlanificacionService(RutaRepositoryPort rutaRepository,
                                NotificacionDespachadorPort notificacion) {
        this.rutaRepository = rutaRepository;
        this.notificacion   = notificacion;
    }

    @Override
    public UUID solicitarRuta(SolicitarRutaCommand command) {
        validarFechaLimite(command.fechaLimiteEntrega());

        ZonaGeografica zona = ZonaGeografica.from(command.latitud(), command.longitud());
        Ruta ruta = rutaRepository
            .buscarRutaActivaPorZona(zona)           // busca estado CREADA
            .orElseGet(() -> crearNuevaRuta(zona));

        ruta.agregarPaquete(command.paqueteId(), command.pesoKg());
        evaluarEscaladoOTransicion(ruta);

        return rutaRepository.guardar(ruta).id();
    }

    private void evaluarEscaladoOTransicion(Ruta ruta) {
        TipoVehiculo tipoActual = ruta.tipoVehiculoRequerido();
        if (tipoActual.porcentajeExcede(ruta.pesoAcumuladoKg(), UMBRAL_CAPACIDAD)) {
            tipoActual.siguienteTipo().ifPresentOrElse(
                ruta::setTipoVehiculoRequerido,
                () -> transicionarAListaParaDespacho(ruta, "capacidad_maxima_alcanzada")
            );
        }
    }

    // ... resto de métodos
}
```

- [ ] T116 [US1] Implementar `RutaRepositoryPort.buscarRutaActivaPorZona(ZonaGeografica zona)`:
  - En `RutaJpaRepository`: `Optional<RutaEntity> findByZonaAndEstado(String zona, EstadoRuta estado)`
  - En `RutaJpaAdapter`: mapear `ZonaGeografica.geohash()` → `String` para la query

- [ ] T117 [US1] **Concurrencia:** El `UNIQUE INDEX PARTIAL` en BD maneja el caso de dos hilos creando ruta en la misma zona simultáneamente. El adaptador JPA debe capturar `DataIntegrityViolationException` y hacer retry consultando la ruta que ya fue creada por el otro hilo:

```java
// infrastructure/adapter/out/persistence/RutaJpaAdapter.java
public Optional<Ruta> buscarRutaActivaPorZona(ZonaGeografica zona) {
    return rutaJpaRepository
        .findByZonaAndEstado(zona.geohash(), EstadoRuta.CREADA)
        .map(rutaMapper::toDomain);
}

// En PlanificacionService, al guardar ruta nueva:
// Si DataIntegrityViolationException → consultar la ruta creada por el otro hilo → agregarle el paquete
```

- [ ] T118 [US1] Implementar `PlanificacionService.despacharManual(UUID rutaId)`
- [ ] T119 [US1] Implementar `PlanificacionService.transicionarRutasVencidas()` — consulta `RutaRepositoryPort.buscarRutasVencidas()` y transiciona cada una a `LISTA_PARA_DESPACHO` con motivo `"vencimiento_plazo"`

---

## Phase 4: Adaptadores de Entrada

### Adaptador REST (para pruebas y Módulo 1 si no usa SQS)

- [ ] T120 [US1] Implementar `PlanificacionController`:
  - `POST /api/planificacion/solicitar-ruta` — requiere `ROLE_SYSTEM`
  - `POST /api/planificacion/rutas/{id}/despacho-manual` — requiere `ROLE_DISPATCHER`
  - `GET /api/planificacion/rutas` — listar rutas activas (`ROLE_DISPATCHER`)

```java
@RestController
@RequestMapping("/api/planificacion")
@RequiredArgsConstructor
public class PlanificacionController {

    private final PlanificacionRouteUseCase planificacion; // ← puerto, no servicio concreto

    @PostMapping("/solicitar-ruta")
    @PreAuthorize("hasRole('SYSTEM')")
    @ResponseStatus(HttpStatus.OK)
    public SolicitarRutaResponse solicitarRuta(@Valid @RequestBody SolicitarRutaRequest request) {
        UUID rutaId = planificacion.solicitarRuta(request.toCommand());
        return new SolicitarRutaResponse(rutaId);
    }
}
```

### Adaptador SQS (producción)

- [ ] T121 [US1] Implementar `SolicitarRutaConsumer`:

```java
// infrastructure/adapter/in/messaging/SolicitarRutaConsumer.java
@Component
@RequiredArgsConstructor
public class SolicitarRutaConsumer {

    private final PlanificacionRouteUseCase planificacion;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.solicitudes-ruta-queue}")
    public void onSolicitarRuta(String payload) {
        SolicitarRutaEvent event = objectMapper.readValue(payload, SolicitarRutaEvent.class);
        planificacion.solicitarRuta(event.toCommand());
    }
}
```

---

## Phase 5: Scheduler — Transición por fecha límite

- [ ] T122 [P] [US1] Implementar `FechaLimiteDespachoScheduler`:

```java
@Component
@RequiredArgsConstructor
public class FechaLimiteDespachoScheduler {

    private final PlanificacionRouteUseCase planificacion;

    // Cada 5 minutos — no fixedRate para evitar solapamiento si la ejecución tarda
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "fecha-limite-despacho", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void transicionarRutasVencidas() {
        planificacion.transicionarRutasVencidas();
    }
}
```

- [ ] T123 [US1] Agregar query en `RutaJpaRepository`:
  ```java
  List<RutaEntity> findByEstadoAndFechaLimiteDespachoLessThanEqual(EstadoRuta estado, LocalDateTime fecha);
  ```
- [ ] T124 [US1] Habilitar `@EnableScheduling` y configurar `ShedLock` con Redis en `AwsSqsConfig`

**Checkpoint: El SGP puede enviar solicitudes vía SQS o REST. Las rutas se crean y escalan correctamente. El scheduler transiciona rutas vencidas.**

---

## Phase N: Polish

- [ ] T125 Documentar endpoints con `@Operation`, `@ApiResponse` de OpenAPI
- [ ] T126 Validaciones `@Valid` + Bean Validation en `SolicitarRutaRequest` (lat/lon obligatorios, peso > 0)
- [ ] T127 Test de integración `PlanificacionIntegrationTest` con Testcontainers + PostgreSQL
  - Flujo: solicitar ruta → verificar creación en BD → solicitar segunda ruta en misma zona → verificar que es la misma ruta con peso sumado
  - Edge case de concurrencia: dos hilos crean ruta en la misma zona simultáneamente → una sola ruta creada

---

## Dependencies & Execution Order

```
PLAN-00 Sprint 0 (Fundación — estructura hexagonal, BD, dominio puro)
    └── PLAN-03 Sprint 1 (Vehículos y Conductores en BD)
            └── Este plan — Sprint 2
                    ├── Phase 2 (Dominio: TipoVehiculo, ZonaGeografica)
                    ├── Phase 3 (PlanificacionService — TDD primero)
                    ├── Phase 4 (Adaptadores de entrada)
                    └── Phase 5 (Scheduler con ShedLock)
```

---

## Notes

- **Geohash precisión 5:** Las celdas son ~4.9 × 4.9 km, compatible con `radio_zona_km = 5` del KEY-ENTITIES. Confirmar con el equipo si Módulo 1 enviará siempre lat/lon (el Módulo 2 calcula el geohash) o si puede enviar el geohash precalculado.
- **Concurrencia sin locks de aplicación:** El `UNIQUE INDEX PARTIAL` en BD es el control de concurrencia. Evitar `SELECT FOR UPDATE` o `@Lock(LockModeType.PESSIMISTIC_WRITE)` — generan deadlocks bajo carga alta.
- **`NotificacionDespachadorPort`** es un stub de log en Sprint 2. Se conectará a WebSocket real en Sprint 5 (Frontend).
- Verificar tests y hacer commit al terminar cada tarea o grupo lógico según el checkpoint.
