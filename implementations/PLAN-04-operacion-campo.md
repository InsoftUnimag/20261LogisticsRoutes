# Implementation Plan: Operación de Campo del Conductor (MOD2-UC-006 / MOD2-UC-007)

**Date:** 2026-04-09 (actualizado)
**Specs:**
- [SPEC-06-consulta-ruta-conductor.md](../specs/SPEC-06-consulta-ruta-conductor.md)
- [SPEC-07-operacion-campo-conductor.md](../specs/SPEC-07-operacion-campo-conductor.md)
- [SPEC-08-integration.md](../specs/SPEC-08-integration.md)

**Arquitectura:** Ver [PLAN-00-arquitectura-master.md](./PLAN-00-arquitectura-master.md)
**Orden de ejecución:** Sprint 4 — depende de PLAN-02 (Sprint 3). Debe haber rutas en estado `CONFIRMADA`.

---

## Summary

Este plan cubre la experiencia completa del conductor en campo: consulta de la ruta asignada con sus paradas ordenadas, inicio del tránsito (transición a `EN_TRANSITO` + eventos al Módulo 1 via **SQS**), registro del resultado de cada parada (exitosa con POD en **S3**, fallida, novedad grave — todos con eventos al Módulo 1 via SQS), cierre manual de ruta y evento `RUTA_CERRADA` al Módulo 3, cierre automático por exceder 2 días en tránsito (scheduler con **ShedLock**), y cierre forzado por el Despachador.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 21 |
| **Framework** | Spring Boot 3.x |
| **Arquitectura** | Hexagonal (Ports & Adapters) — ver PLAN-00 |
| **Primary Dependencies** | Spring Web, Spring Data JPA, Spring Scheduler, Spring Cloud AWS (SQS + S3), ShedLock |
| **Storage** | PostgreSQL + Amazon S3 (fotos POD) |
| **Mensajería** | Amazon SQS — eventos a Módulo 1 y Módulo 3 |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Performance Goals** | Evento `PAQUETE_EN_TRANSITO` a Módulo 1 en < 5 seg. Evento `RUTA_CERRADA` a Módulo 3 en < 5 seg. |
| **Constraints** | POD (foto) obligatorio para parada exitosa. Timestamps del conductor para soporte offline. Conductor no interactúa con Módulo 3. |

---

## Project Structure

```
domain/
├── model/
│   └── Parada.java                                        [NUEVO]
├── enums/
│   ├── EstadoParada.java                                  [existente — PLAN-00]
│   ├── MotivoNovedad.java                                 [existente — PLAN-00]
│   ├── OrigenParada.java                                  [existente — PLAN-00]
│   └── TipoCierre.java                                    [existente — PLAN-00]
└── exception/
    ├── ParadaSinPODException.java                         [existente — PLAN-00]
    ├── ParadaNoEncontradaException.java                   [existente — PLAN-00]
    └── RutaNoEnTransitoException.java                     [existente — PLAN-00]

application/
├── port/
│   └── in/
│       ├── ConsultarRutaActivaPort.java                   [existente — PLAN-00]
│       ├── IniciarTransitoPort.java                       [existente — PLAN-00]
│       ├── RegistrarParadaPort.java                       [existente — PLAN-00]
│       ├── CerrarRutaManualPort.java                      [existente — PLAN-00]
│       ├── ForzarCierreRutaPort.java                      [existente — PLAN-00]
│       └── CerrarRutasExcedidasPort.java                  [existente — PLAN-00]
└── usecase/
    ├── ConsultarRutaActivaUseCase.java                    [NUEVO — implements ConsultarRutaActivaPort]
    ├── IniciarTransitoUseCase.java                        [NUEVO — implements IniciarTransitoPort]
    ├── RegistrarParadaUseCase.java                        [NUEVO — implements RegistrarParadaPort]
    ├── CerrarRutaManualUseCase.java                       [NUEVO — implements CerrarRutaManualPort]
    ├── ForzarCierreRutaUseCase.java                       [NUEVO — implements ForzarCierreRutaPort]
    └── CerrarRutasExcedidasUseCase.java                   [NUEVO — implements CerrarRutasExcedidasPort]

infrastructure/
├── adapter/
│   ├── in/web/
│   │   ├── ConductorOperacionController.java              [NUEVO — inyecta puertos del conductor]
│   │   └── DespachoController.java                        (ya existe PLAN-02 — inyecta ForzarCierreRutaPort)
│   └── out/
│       ├── persistence/
│       │   └── ParadaJpaAdapter.java                      [existente — PLAN-00, se extiende]
│       ├── messaging/
│       │   ├── SqsIntegracionModulo1Adapter.java          [NUEVO — implements IntegracionModulo1Port]
│       │   └── SqsIntegracionModulo3Adapter.java          [NUEVO — implements IntegracionModulo3Port]
│       └── storage/
│           └── S3AlmacenamientoAdapter.java               [NUEVO — implements AlmacenamientoArchivoPort]
├── persistence/
│   ├── entity/
│   │   └── ParadaEntity.java                              [existente — PLAN-00]
│   └── repository/
│       └── ParadaJpaRepository.java                       [existente — PLAN-00]
├── scheduler/
│   └── CierreAutomaticoScheduler.java                     [NUEVO — llama a CerrarRutasExcedidasPort]
└── dto/
    ├── request/
    │   ├── RegistrarParadaRequest.java                    [NUEVO]
    │   └── CierreRutaRequest.java                         [NUEVO]
    └── response/
        └── RutaConductorResponse.java                     [NUEVO]
```

---

## Phase 1: Prerequisitos

- [ ] T401 Verificar que existen rutas en estado `CONFIRMADA` con conductor y vehículo asignados (PLAN-02 completo)
- [ ] T402 Verificar que `ConsultarRutaActivaPort`, `IniciarTransitoPort`, `RegistrarParadaPort`, `CerrarRutaManualPort`, `ForzarCierreRutaPort`, `CerrarRutasExcedidasPort` están en `application/port/in/` y que `ParadaRepositoryPort`, `IntegracionModulo1Port`, `IntegracionModulo3Port`, `AlmacenamientoArchivoPort` están en `application/port/out/` (PLAN-00 completo)
- [ ] T403 Verificar configuración AWS (credenciales SQS + S3 en `application-dev.yml` / Secrets Manager en prod)

---

## Phase 2: Entidad Parada y Adaptadores de Infraestructura

**Purpose:** Sin la entidad `Parada` y los adaptadores SQS/S3, ninguna historia de campo puede implementarse.

⚠️ **CRÍTICO: Fase bloqueante.**

### Entidad de dominio Parada

- [ ] T404 [P] Crear `Parada` en `domain/model/Parada.java`:

```java
public class Parada {
    private UUID id;
    private UUID rutaId;
    private UUID paqueteId;
    private int orden;
    private String direccion;
    private double latitud;
    private double longitud;
    private String tipoMercancia;
    private String metodoPago;
    private Instant fechaLimiteEntrega;
    private EstadoParada estado;
    private MotivoNovedad motivoNovedad;
    private Instant fechaHoraGestion;   // timestamp del conductor (offline)
    private String firmaReceptorUrl;
    private String fotoEvidenciaUrl;
    private String nombreReceptor;
    private OrigenParada origen;

    // Métodos de dominio
    public void marcarExitosa(String fotoUrl, String firmaUrl, String nombreReceptor, Instant fechaAccion) {
        if (fotoUrl == null || fotoUrl.isBlank()) throw new ParadaSinPODException(this.paqueteId);
        this.estado = EstadoParada.EXITOSA;
        this.fotoEvidenciaUrl = fotoUrl;
        this.firmaReceptorUrl = firmaUrl;
        this.nombreReceptor = nombreReceptor;
        this.fechaHoraGestion = fechaAccion;  // timestamp del conductor, no now()
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarFallida(MotivoNovedad motivo, Instant fechaAccion) {
        this.estado = EstadoParada.FALLIDA;
        this.motivoNovedad = motivo;
        this.fechaHoraGestion = fechaAccion;
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarNovedad(MotivoNovedad tipoNovedad, Instant fechaAccion) {
        this.estado = EstadoParada.NOVEDAD;
        this.motivoNovedad = tipoNovedad;
        this.fechaHoraGestion = fechaAccion;
        this.origen = OrigenParada.CONDUCTOR;
    }

    public void marcarSinGestion() {
        this.estado = EstadoParada.SIN_GESTION_CONDUCTOR;
        this.fechaHoraGestion = Instant.now();
        this.origen = OrigenParada.SISTEMA;
    }
}
```

- [ ] T405 [P] Extender `ParadaRepositoryPort`:
  ```java
  List<Parada> buscarPorRutaId(UUID rutaId);
  List<Parada> buscarPendientesPorRutaId(UUID rutaId);
  Parada guardar(Parada parada);
  ```

### Adaptadores SQS (reemplazan IntegracionExternaService)

- [ ] T406 [P] Implementar `SqsIntegracionModulo1Adapter implements IntegracionModulo1Port`:

```java
@Component
@RequiredArgsConstructor
public class SqsIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.eventos-paquete-queue}")
    private String eventosPaqueteQueue;

    @Override
    public void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHoraEvento) {
        String payload = objectMapper.writeValueAsString(Map.of(
            "tipo_evento", "PAQUETE_EN_TRANSITO",
            "paquete_id", paqueteId,
            "ruta_id", rutaId,
            "fecha_hora_evento", fechaHoraEvento
        ));
        sqsTemplate.send(eventosPaqueteQueue, payload);
        log.info("Published PAQUETE_EN_TRANSITO for paquete={}", paqueteId);
    }

    @Override
    public void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fecha,
                                        String urlFoto, String urlFirma) { ... }

    @Override
    public void publishParadaFallida(UUID paqueteId, UUID rutaId, Instant fecha,
                                     MotivoNovedad motivo) { ... }

    @Override
    public void publishNovedadGrave(UUID paqueteId, UUID rutaId, Instant fecha,
                                    MotivoNovedad tipoNovedad) { ... }

    @Override
    public void publishParadasSinGestionar(UUID rutaId, TipoCierre tipoCierre,
                                           List<UUID> paqueteIds) { ... }
}
```

- [ ] T407 [P] Implementar `SqsIntegracionModulo3Adapter implements IntegracionModulo3Port`:
  - Serializar payload completo de `RUTA_CERRADA` según SPEC-08 sección 4
  - Publicar en `cierre-ruta-queue`

- [ ] T408 [P] Implementar `S3AlmacenamientoAdapter implements AlmacenamientoArchivoPort`:

```java
@Component
@RequiredArgsConstructor
public class S3AlmacenamientoAdapter implements AlmacenamientoArchivoPort {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-pod}")
    private String bucket;

    @Override
    public String almacenarFoto(UUID paradaId, byte[] foto, String contentType) {
        String key = "pod/fotos/" + paradaId + "/" + UUID.randomUUID();
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
            RequestBody.fromBytes(foto)
        );
        return "s3://" + bucket + "/" + key; // URL interna; presigned URL generada al consultar
    }
    // ...
}
```

**Checkpoint: Entidad Parada, adaptadores SQS y S3 listos. Se puede implementar operación de campo.**

---

## Phase 3: User Story 6 — Consulta de Ruta y Tránsito (Priority: P2)

**Goal:** Conductor consulta su ruta con paradas ordenadas e inicia el tránsito.

**Independent Test:** Con ruta en `CONFIRMADA`, `GET /api/conductor/ruta-activa` → retorna paradas ordenadas. `POST /api/conductor/rutas/{id}/iniciar-transito` → ruta `EN_TRANSITO` + `PAQUETE_EN_TRANSITO` publicado en SQS por cada paquete.

### Tests (TDD)

- [ ] T409 [P] [US6] `ConsultarRutaActivaUseCaseTest` — consulta retorna ruta con paradas ordenadas:
  - Mock de `RutaRepositoryPort`: retorna ruta `CONFIRMADA` con conductorId correcto
  - Mock de `ParadaRepositoryPort`: retorna lista de paradas ordenadas
  - Resultado: `RutaConductorView` con paradas en orden correcto

- [ ] T410 [P] [US6] `ConsultarRutaActivaUseCaseTest` — sin ruta asignada → retorna `RutaConductorView.sinRutaAsignada()`

- [ ] T411 [P] [US6] `IniciarTransitoUseCaseTest` — iniciar tránsito:
  - Ruta en `CONFIRMADA` → transiciona a `EN_TRANSITO`, registra `fechaHoraInicio`
  - `IntegracionModulo1Port.publishPaqueteEnTransito()` llamado una vez por cada parada

### Implementación

- [ ] T412 [P] [US6] Implementar `ConsultarRutaActivaUseCase implements ConsultarRutaActivaPort` e `IniciarTransitoUseCase implements IniciarTransitoPort`:

```java
// application/usecase/ConsultarRutaActivaUseCase.java
@Service
@RequiredArgsConstructor
public class ConsultarRutaActivaUseCase implements ConsultarRutaActivaPort {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;

    @Override
    public RutaConductorView ejecutar(UUID conductorId) {
        return rutaRepository.buscarRutaActivaDeConductor(conductorId)
            .map(ruta -> {
                List<Parada> paradas = paradaRepository.buscarPorRutaId(ruta.id());
                return new RutaConductorView(ruta, paradas);
            })
            .orElse(RutaConductorView.sinRutaAsignada());
    }
}

// application/usecase/IniciarTransitoUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class IniciarTransitoUseCase implements IniciarTransitoPort {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionM1;

    @Override
    public void ejecutar(UUID rutaId, UUID conductorId) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
            .orElseThrow(() -> new RutaNoEncontradaException(rutaId));
        ruta.iniciarTransito(); // cambia estado a EN_TRANSITO, registra fechaHoraInicio
        rutaRepository.guardar(ruta);

        Instant ahora = Instant.now();
        paradaRepository.buscarPorRutaId(rutaId).forEach(parada ->
            integracionM1.publishPaqueteEnTransito(parada.paqueteId(), rutaId, ahora)
        );
    }
}
```

- [ ] T413 [US6] Extender `RutaRepositoryPort`:
  ```java
  Optional<Ruta> buscarRutaActivaDeConductor(UUID conductorId);
  // Query: estado IN ('CONFIRMADA', 'EN_TRANSITO') AND conductor_id = :conductorId
  ```

- [ ] T414 [US6] Implementar `ConductorOperacionController`:

```java
@RestController
@RequestMapping("/api/conductor")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
public class ConductorOperacionController {

    // Cada campo inyecta el puerto individual que necesita — SRP en el controller
    private final ConsultarRutaActivaPort consultarRutaActiva;
    private final IniciarTransitoPort iniciarTransito;
    private final RegistrarParadaPort registrarParada;
    private final CerrarRutaManualPort cerrarRuta;

    @GetMapping("/ruta-activa")
    public RutaConductorResponse rutaActiva(Authentication auth) {
        UUID conductorId = extractConductorId(auth);
        return consultarRutaActiva.ejecutar(conductorId).toResponse();
    }

    @PostMapping("/rutas/{id}/iniciar-transito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarTransito(@PathVariable UUID id, Authentication auth) {
        iniciarTransito.ejecutar(id, extractConductorId(auth));
    }

    @PostMapping("/rutas/{id}/paradas/{paqueteId}/resultado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarResultado(@PathVariable UUID id,
                                   @PathVariable UUID paqueteId,
                                   @Valid @RequestBody RegistrarParadaRequest request) {
        registrarParada.ejecutar(id, request.toCommand(paqueteId));
    }

    @PostMapping("/rutas/{id}/cerrar")
    public void cerrarRuta(@PathVariable UUID id,
                           @Valid @RequestBody CierreRutaRequest request) {
        cerrarRuta.ejecutar(id, request.confirmarConPendientes());
    }
}
```

**Checkpoint: Conductor puede consultar su ruta e iniciarla. Módulo 1 recibe notificación por SQS.**

---

## Phase 4: User Story 7a — Registro de Resultado de Parada (Priority: P2)

**Goal:** Conductor registra el resultado de cada parada. Foto POD en S3. Notificación asíncrona a Módulo 1.

**Independent Test:** Ruta `EN_TRANSITO`. `POST /api/conductor/rutas/{id}/paradas/{paqueteId}/resultado` con resultado EXITOSA y URL de foto → parada `EXITOSA` en BD + mensaje SQS `PAQUETE_ENTREGADO`.

### Tests (TDD)

- [ ] T415 [P] [US7a] `RegistrarParadaUseCaseTest` — parada EXITOSA con foto:
  - `RegistrarParadaCommand` con resultado EXITOSA, `urlFoto != null`, `fechaHoraAccion` del conductor
  - Parada → `EXITOSA`. `IntegracionModulo1Port.publishPaqueteEntregado()` llamado. Timestamp = `fechaHoraAccion` (no `now()`)

- [ ] T416 [P] [US7a] `RegistrarParadaUseCaseTest` — parada EXITOSA SIN foto → `ParadaSinPODException`:
  - `urlFoto = null` → excepción lanzada antes de persistir

- [ ] T417 [P] [US7a] `RegistrarParadaUseCaseTest` — parada FALLIDA con motivo:
  - `motivoNovedad = CLIENTE_AUSENTE` → parada `FALLIDA`. `publishParadaFallida()` llamado.

- [ ] T418 [P] [US7a] `RegistrarParadaUseCaseTest` — novedad grave:
  - `tipoNovedad = DAÑADO_EN_RUTA` → parada `NOVEDAD`. `publishNovedadGrave()` llamado.

- [ ] T419 [US7a] `RegistrarParadaUseCaseTest` — offline timestamp:
  - El campo `fechaHoraAccion` del `RegistrarParadaCommand` (enviado por el conductor) se usa como `fechaHoraGestion`, no `Instant.now()`

### Implementación

- [ ] T420 [P] [US7a] Implementar `RegistrarParadaUseCase implements RegistrarParadaPort`:

```java
// application/usecase/RegistrarParadaUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class RegistrarParadaUseCase implements RegistrarParadaPort {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final IntegracionModulo1Port integracionM1;

    @Override
    public void ejecutar(UUID rutaId, RegistrarParadaCommand command) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
            .filter(r -> r.estado() == EstadoRuta.EN_TRANSITO)
            .orElseThrow(() -> new RutaNoEnTransitoException(rutaId));

        Parada parada = paradaRepository.buscarPorRutaYPaquete(rutaId, command.paqueteId())
            .orElseThrow(() -> new ParadaNoEncontradaException(command.paqueteId()));

        switch (command.resultado()) {
            case EXITOSA -> {
                parada.marcarExitosa(command.urlFoto(), command.urlFirma(),
                                     command.nombreReceptor(), command.fechaHoraAccion());
                integracionM1.publishPaqueteEntregado(command.paqueteId(), rutaId,
                    command.fechaHoraAccion(), command.urlFoto(), command.urlFirma());
            }
            case FALLIDA -> {
                parada.marcarFallida(command.motivoNovedad(), command.fechaHoraAccion());
                integracionM1.publishParadaFallida(command.paqueteId(), rutaId,
                    command.fechaHoraAccion(), command.motivoNovedad());
            }
            case NOVEDAD -> {
                parada.marcarNovedad(command.motivoNovedad(), command.fechaHoraAccion());
                integracionM1.publishNovedadGrave(command.paqueteId(), rutaId,
                    command.fechaHoraAccion(), command.motivoNovedad());
            }
        }

        paradaRepository.guardar(parada);
    }
}
```

- [ ] T421 [US7a] `RegistrarParadaRequest` record:
  ```java
  public record RegistrarParadaRequest(
      @NotNull ResultadoParada resultado,
      MotivoNovedad motivoNovedad,         // requerido si resultado != EXITOSA
      String urlFoto,                       // requerido si resultado == EXITOSA
      String urlFirma,
      String nombreReceptor,
      @NotNull Instant fechaHoraAccion     // timestamp del conductor — soporte offline
  ) {}
  ```

**Checkpoint: Conductor gestiona paradas. Módulo 1 recibe eventos por SQS en tiempo real.**

---

## Phase 5: User Story 7b — Cierre de Ruta (Priority: P1)

**Goal:** Conductor cierra la ruta. Sistema genera evento `RUTA_CERRADA` a Módulo 3. Scheduler cierra automáticamente rutas excedidas.

**Independent Test:** Gestionar todas las paradas de una ruta y llamar `POST /api/conductor/rutas/{id}/cerrar` → ruta `CERRADA_MANUAL` + mensaje SQS `RUTA_CERRADA` con payload completo según SPEC-08.

### Tests (TDD)

- [ ] T422 [P] [US7b] `CerrarRutaManualUseCaseTest` — cierre exitoso con todas las paradas gestionadas:
  - No hay paradas `PENDIENTE` → ruta `CERRADA_MANUAL`. `IntegracionModulo3Port.publishRutaCerrada()` llamado.
  - Vehículo pasa a `DISPONIBLE` y conductor pasa a `ACTIVO`.

- [ ] T423 [P] [US7b] `CerrarRutaManualUseCaseTest` — cierre con paradas pendientes y `confirmarConPendientes = false`:
  - Lanza excepción o retorna estado indicando que hay pendientes. No cierra la ruta.

- [ ] T424 [P] [US7b] `CerrarRutaManualUseCaseTest` — cierre con `confirmarConPendientes = true`:
  - Paradas `PENDIENTE` → marcadas `SIN_GESTION_CONDUCTOR` con `origen = SISTEMA`
  - `IntegracionModulo1Port.publishParadasSinGestionar()` llamado
  - `IntegracionModulo3Port.publishRutaCerrada()` llamado

- [ ] T425 [US7b] `ForzarCierreRutaUseCaseTest` — cierre forzado por Despachador:
  - `tipoCierre = FORZADO_DESPACHADOR` → ruta `CERRADA_FORZADA`

- [ ] T426 [P] [US7b] `CierreAutomaticoSchedulerTest` — rutas > 2 días `EN_TRANSITO` → cierre automático:
  - MockBean del puerto `CerrarRutasExcedidasPort`. Verificar que `ejecutar()` es llamado por el scheduler.

- [ ] T427 [US7b] `CerrarRutaManualUseCaseTest` — payload `RUTA_CERRADA` incluye `modelo_contrato` del conductor:
  - Verificar que `IntegracionModulo3Port.publishRutaCerrada()` recibe evento con `conductor.modeloContrato` según SPEC-08

### Implementación

- [ ] T428 [P] [US7b] Implementar los use cases de cierre: `CerrarRutaManualUseCase`, `ForzarCierreRutaUseCase`, `CerrarRutasExcedidasUseCase`:

> **Nota de arquitectura:** Los tres use cases comparten la lógica del cierre. Extraerla a un método privado de apoyo o a un helper de dominio (no a un servicio Spring) para evitar dependencias entre use cases.

```java
// application/usecase/CerrarRutaManualUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class CerrarRutaManualUseCase implements CerrarRutaManualPort {

    private final RutaRepositoryPort rutaRepository;
    private final ParadaRepositoryPort paradaRepository;
    private final ConductorRepositoryPort conductorRepository;
    private final VehiculoRepositoryPort vehiculoRepository;
    private final IntegracionModulo1Port integracionM1;
    private final IntegracionModulo3Port integracionM3;

    @Override
    public void ejecutar(UUID rutaId, boolean confirmarConPendientes) {
        cerrar(rutaId, confirmarConPendientes, TipoCierre.MANUAL);
    }

    // Lógica de cierre compartida (método package-private para reutilizar en los otros use cases)
    void cerrar(UUID rutaId, boolean confirmarConPendientes, TipoCierre tipoCierre) {
        Ruta ruta = rutaRepository.buscarPorId(rutaId)
            .filter(r -> r.estado() == EstadoRuta.EN_TRANSITO)
            .orElseThrow(() -> new RutaNoEnTransitoException(rutaId));

        List<Parada> pendientes = paradaRepository.buscarPendientesPorRutaId(rutaId);

        if (!pendientes.isEmpty() && !confirmarConPendientes) {
            return CierreRutaResult.conPendientes(pendientes);
        }

        if (!pendientes.isEmpty()) {
            pendientes.forEach(Parada::marcarSinGestion);
            paradaRepository.guardarTodas(pendientes);
            List<UUID> paqueteIdsSinGestion = pendientes.stream()
                .map(Parada::paqueteId).toList();
            integracionM1.publishParadasSinGestionar(rutaId, tipoCierre, paqueteIdsSinGestion);
        }

        EstadoRuta estadoCierre = switch (tipoCierre) {
            case MANUAL -> EstadoRuta.CERRADA_MANUAL;
            case AUTOMATICO -> EstadoRuta.CERRADA_AUTOMATICA;
            case FORZADO_DESPACHADOR -> EstadoRuta.CERRADA_FORZADA;
        };

        ruta.cerrar(estadoCierre, Instant.now(), tipoCierre);
        rutaRepository.guardar(ruta);

        publishRutaCerrada(ruta);

        // Liberar vehículo y conductor (SPEC-07 scenarios 1, 2, 3)
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(ruta.vehiculoId()).orElseThrow();
        Conductor conductor = conductorRepository.buscarPorId(ruta.conductorId()).orElseThrow();
        vehiculo.marcarDisponible();
        conductor.marcarActivo();
        vehiculoRepository.guardar(vehiculo);
        conductorRepository.guardar(conductor);

    }

    private void publishRutaCerrada(Ruta ruta) {
        Conductor conductor = conductorRepository.buscarPorId(ruta.conductorId()).orElseThrow();
        Vehiculo vehiculo = vehiculoRepository.buscarPorId(ruta.vehiculoId()).orElseThrow();
        List<Parada> paradas = paradaRepository.buscarPorRutaId(ruta.id());

        RutaCerradaEvent event = RutaCerradaEvent.from(ruta, conductor, vehiculo, paradas);
        integracionM3.publishRutaCerrada(event); // payload exacto de SPEC-08 sección 4
    }
}
```

- [ ] T428b [P] [US7b] Implementar `ForzarCierreRutaUseCase implements ForzarCierreRutaPort`:

```java
// application/usecase/ForzarCierreRutaUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class ForzarCierreRutaUseCase implements ForzarCierreRutaPort {

    private final CerrarRutaManualUseCase cierreHelper; // reutiliza lógica de cierre

    @Override
    public void ejecutar(UUID rutaId) {
        cierreHelper.cerrar(rutaId, true, TipoCierre.FORZADO_DESPACHADOR);
    }
}
```

- [ ] T428c [P] [US7b] Implementar `CerrarRutasExcedidasUseCase implements CerrarRutasExcedidasPort`:

```java
// application/usecase/CerrarRutasExcedidasUseCase.java
@Service
@Transactional
@RequiredArgsConstructor
public class CerrarRutasExcedidasUseCase implements CerrarRutasExcedidasPort {

    private final RutaRepositoryPort rutaRepository;
    private final CerrarRutaManualUseCase cierreHelper;
    private final NotificacionDespachadorPort notificacion;

    @Override
    public void ejecutar() {
        Instant limiteTransito = Instant.now().minus(2, ChronoUnit.DAYS);
        List<Ruta> excedidas = rutaRepository.buscarRutasEnTransitoExcedidas(limiteTransito);
        excedidas.forEach(ruta -> {
            cierreHelper.cerrar(ruta.id(), true, TipoCierre.AUTOMATICO);
            notificacion.notificarAlertaPrioritaria(
                "Ruta " + ruta.id() + " cerrada automáticamente por exceder 2 días en tránsito"
            );
        });
    }
}
```

- [ ] T429 [US7b] Endpoint `POST /api/conductor/rutas/{id}/cerrar` ya implementado en `ConductorOperacionController` (ver T414 — inyecta `CerrarRutaManualPort`)
- [ ] T430 [US7b] Endpoint `POST /api/despacho/rutas/{id}/forzar-cierre` ya implementado en `DespachoController` (ver PLAN-02 T216 — inyecta `ForzarCierreRutaPort`)

- [ ] T432 [P] [US7b] Implementar `CierreAutomaticoScheduler`:

```java
@Component
@RequiredArgsConstructor
public class CierreAutomaticoScheduler {

    private final CerrarRutasExcedidasPort cerrarRutasExcedidas; // ← puerto individual

    // Corre cada hora (no fixedRate — evita solapamiento si la ejecución tarda)
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "cierre-automatico-scheduler", lockAtMostFor = "50m", lockAtLeastFor = "5m")
    public void cerrarRutasExcedidas() {
        cerrarRutasExcedidas.ejecutar();
    }
}
```

- [ ] T433 [US7b] Extender `RutaRepositoryPort` con:
  ```java
  List<Ruta> buscarRutasEnTransitoExcedidas(Instant fechaLimite);
  // Query: estado = 'EN_TRANSITO' AND fecha_hora_inicio <= :fechaLimite
  ```

- [ ] T434 [US7b] Extender `RutaJpaRepository`:
  ```java
  List<RutaEntity> findByEstadoAndFechaHoraInicioLessThanEqual(EstadoRuta estado, Instant fecha);
  ```

**Checkpoint: Flujo completo de cierre funciona — manual, automático (scheduler con ShedLock) y forzado. Módulo 3 recibe evento `RUTA_CERRADA` con payload completo de SPEC-08.**

---

## Phase 6: RutaCerradaEvent — payload exacto de SPEC-08

- [ ] T435 [P] Crear `RutaCerradaEvent` record que serializa exactamente el JSON de SPEC-08 sección 4:

```java
public record RutaCerradaEvent(
    @JsonProperty("tipo_evento") String tipoEvento,
    @JsonProperty("ruta_id") UUID rutaId,
    @JsonProperty("tipo_cierre") String tipoCierre,
    @JsonProperty("fecha_hora_inicio_transito") Instant fechaHoraInicioTransito,
    @JsonProperty("fecha_hora_cierre") Instant fechaHoraCierre,
    ConductorInfo conductor,
    VehiculoInfo vehiculo,
    List<ParadaInfo> paradas
) {
    public record ConductorInfo(
        @JsonProperty("conductor_id") UUID conductorId,
        String nombre,
        @JsonProperty("modelo_contrato") String modeloContrato  // ← campo que faltaba
    ) {}
    public record VehiculoInfo(
        @JsonProperty("vehiculo_id") UUID vehiculoId,
        String placa,
        String tipo
    ) {}
    public record ParadaInfo(
        @JsonProperty("paquete_id") UUID paqueteId,
        String estado,
        @JsonProperty("motivo_no_entrega") String motivoNoEntrega,
        @JsonProperty("fecha_hora_gestion") Instant fechaHoraGestion
    ) {}

    public static RutaCerradaEvent from(Ruta ruta, Conductor conductor,
                                         Vehiculo vehiculo, List<Parada> paradas) {
        return new RutaCerradaEvent(
            "RUTA_CERRADA", ruta.id(), ruta.tipoCierre().name(),
            ruta.fechaHoraInicio(), ruta.fechaHoraCierre(),
            new ConductorInfo(conductor.id(), conductor.nombre(), conductor.modeloContrato().name()),
            new VehiculoInfo(vehiculo.id(), vehiculo.placa(), vehiculo.tipo().name()),
            paradas.stream().map(p -> new ParadaInfo(
                p.paqueteId(), p.estado().name(),
                p.motivoNovedad() != null ? p.motivoNovedad().name() : null,
                p.fechaHoraGestion()
            )).toList()
        );
    }
}
```

---

## Phase N: Polish

- [ ] T436 Documentar todos los endpoints de operación de campo con `@Operation`, `@ApiResponse`
- [ ] T437 Verificar que `fechaHoraAccion` del conductor se usa correctamente en **todos** los registros (no `now()`)
- [ ] T438 Validaciones `@Valid` en `RegistrarParadaRequest` y `CierreRutaRequest`
- [ ] T439 Test de integración E2E `OperacionCampoIntegrationTest` con Testcontainers:
  - Flujo completo: ruta `CONFIRMADA` → iniciar tránsito → registrar paradas → cerrar ruta → verificar payload a SQS

---

## Dependencies & Execution Order

```
PLAN-00 Sprint 0 (Fundación hexagonal)
    └── PLAN-03 Sprint 1 (Vehiculo, Conductor, modelo_contrato)
            └── PLAN-01 Sprint 2 (Rutas y planificación)
                    └── PLAN-02 Sprint 3 (Rutas CONFIRMADAS con conductor y vehículo)
                            └── Este plan — Sprint 4
                                    ├── Phase 2 (Parada + SQS/S3) — BLOQUEA todo
                                    ├── Phase 3 (ConsultarRutaActivaUseCase + IniciarTransitoUseCase)
                                    ├── Phase 4 (RegistrarParadaUseCase)
                                    ├── Phase 5 (CerrarRutaManualUseCase + ForzarCierreRutaUseCase + CerrarRutasExcedidasUseCase + Scheduler)
                                    └── Phase 6 (RutaCerradaEvent completo)
```

---

## Notes
- **Soporte offline:** El backend recibe `fechaHoraAccion` del conductor. El frontend (Sprint 5) es responsable del almacenamiento local en IndexedDB y la cola de sincronización. El backend no distingue requests online vs offline — simplemente confía en `fechaHoraAccion`.
- **Fotos POD:** El conductor primero sube la foto (`POST /foto`), recibe la URL de S3, luego envía el resultado de la parada con esa URL. No se envían fotos en multipart junto al resultado (evita timeouts en conexiones lentas de campo).
- El payload de `RutaCerradaEvent` debe seguir **exactamente** la estructura de SPEC-08 sección 4. El `modelo_contrato` del conductor es requerido — fue agregado en PLAN-03.
