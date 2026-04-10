# PLAN-00 — Arquitectura Maestra y Fundación del Proyecto

**Date:** 2026-04-09
**Specs relacionadas:** (SPEC-01 a SPEC-08)
**Planes que gobiernan:** PLAN-01, PLAN-02, PLAN-03, PLAN-04

---


## Decisiones Arquitectónicas

### 1. Arquitectura Hexagonal (Ports & Adapters)

Las dependencias apuntan siempre **hacia adentro**. La regla es absoluta.

```
┌──────────────────────────────────────────────────────┐
│  INFRASTRUCTURE (Adaptadores Web, JPA, SQS, S3)     │
│  ┌────────────────────────────────────────────────┐  │
│  │  APPLICATION (Servicios — casos de uso)        │  │
│  │  ┌──────────────────────────────────────────┐  │  │
│  │  │  DOMAIN (Entidades + Puertos)            │  │  │
│  │  │  Sin imports de Spring. POJO puro.       │  │  │
│  │  └──────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

**Regla:** `domain` no importa nada de `application` ni de `infrastructure`.  
`application` importa `domain` pero nunca `infrastructure`.  
`infrastructure` puede importar ambas.

### 2. Estrategia de Zona Geográfica

**Decisión: Geohash de precisión 5** (~4.9 km × 4.9 km, coincide con `radio_zona_km = 5 km` de KEY-ENTITIES).

```java
// Value Object en domain/valueobject/ZonaGeografica.java
public record ZonaGeografica(String geohash) {
    public static ZonaGeografica from(double latitud, double longitud) {
        return new ZonaGeografica(GeoHash.geoHashStringWithCharacterPrecision(latitud, longitud, 5));
    }
}
```

**Beneficio:** El geohash es un `String` comparable con `=` en SQL, indexable con B-Tree estándar.  
El `UNIQUE INDEX PARTIAL` sobre `zona WHERE estado = 'CREADA'` resuelve la concurrencia **a nivel de BD** sin locks de aplicación.

### 3. Mensajería Asíncrona

**Decisión: Amazon SQS**

| Cola | Emisor | Receptor |
|---|---|---|
| `solicitudes-ruta-queue` | Módulo 1 (SGP) | Módulo 2 — `SolicitarRutaConsumer` |
| `eventos-paquete-queue` | Módulo 2 | Módulo 1 (SGP) |
| `cierre-ruta-queue` | Módulo 2 | Módulo 3 (Facturación) |

Reemplaza el `IntegracionExternaService` stub de PLAN-04 con adaptadores SQS reales.

### 4. Almacenamiento de Fotos POD

**Decisión: Amazon S3** con URLs pre-firmadas (TTL 24h).  
El conductor sube la foto → el backend la guarda en S3 → retorna URL pre-firmada → se almacena en `paradas.foto_evidencia_url`.

### 5. Autenticación

**Decisión: Spring Security + JWT**

| Rol | Endpoints |
|---|---|
| `ROLE_DISPATCHER` | `/api/despacho/**` |
| `ROLE_DRIVER` | `/api/conductor/**` |
| `ROLE_FLEET_ADMIN` | `/api/vehiculos/**`, `/api/conductores/**`, `/api/flota/**` |
| `ROLE_SYSTEM` | `/api/planificacion/solicitar-ruta` |

---

## Estructura de Paquetes (Hexagonal)

```
src/main/java/com/logistics/routes/
│
├── domain/                              ← Sin dependencias externas. POJO puro.
│   ├── model/
│   │   ├── Ruta.java
│   │   ├── Vehiculo.java
│   │   ├── Conductor.java
│   │   ├── Parada.java
│   │   └── HistorialAsignacion.java
│   ├── valueobject/
│   │   └── ZonaGeografica.java          ← Encapsula geohash + coordenadas
│   ├── enums/
│   │   ├── EstadoRuta.java
│   │   ├── TipoVehiculo.java            ← con siguienteTipo() y capacidadKg()
│   │   ├── EstadoVehiculo.java
│   │   ├── EstadoConductor.java
│   │   ├── EstadoParada.java
│   │   │   └── PENDIENTE, EXITOSA, FALLIDA, NOVEDAD, SIN_GESTION_CONDUCTOR, EXCLUIDA_DESPACHO
│   │   ├── MotivoNovedad.java
│   │   ├── OrigenParada.java
│   │   └── TipoCierre.java
│   ├── exception/                       ← Sin mensajes HTTP. Solo de dominio.
│   │   ├── RutaNoEncontradaException.java
│   │   ├── VehiculoEnTransitoException.java
│   │   ├── ConductorYaAsignadoException.java
│   │   ├── FechaLimiteVencidaException.java
│   │   ├── PlacaDuplicadaException.java
│   │   └── ParadaSinPODException.java
│   └── port/
│       ├── in/                          ← Driving Ports (interfaces de casos de uso)
│       │   ├── PlanificacionRouteUseCase.java
│       │   ├── DespachoRouteUseCase.java
│       │   ├── GestionFlotaUseCase.java
│       │   ├── GestionConductoresUseCase.java
│       │   └── OperacionCampoUseCase.java
│       └── out/                         ← Driven Ports (interfaces de infraestructura)
│           ├── RutaRepositoryPort.java
│           ├── VehiculoRepositoryPort.java
│           ├── ConductorRepositoryPort.java
│           ├── ParadaRepositoryPort.java
│           ├── HistorialAsignacionRepositoryPort.java
│           ├── NotificacionDespachadorPort.java
│           ├── IntegracionModulo1Port.java
│           ├── IntegracionModulo3Port.java
│           └── AlmacenamientoArchivoPort.java
│
├── application/                         ← Implementan los puertos de entrada
│   ├── planificacion/
│   │   └── PlanificacionService.java    ← implements PlanificacionRouteUseCase
│   ├── despacho/
│   │   └── DespachoService.java         ← implements DespachoRouteUseCase
│   ├── flota/
│   │   ├── VehiculoService.java         ← implements GestionFlotaUseCase
│   │   └── ConductorService.java        ← implements GestionConductoresUseCase
│   └── campo/
│       ├── ConductorOperacionService.java ← implements OperacionCampoUseCase
│       └── CierreRutaService.java
│
└── infrastructure/                      ← Implementan los puertos de salida
    ├── adapter/
    │   ├── in/
    │   │   ├── web/                     ← Controllers REST
    │   │   │   ├── PlanificacionController.java
    │   │   │   ├── DespachoController.java
    │   │   │   ├── VehiculoController.java
    │   │   │   ├── ConductorController.java         ← gestión admin (PLAN-03)
    │   │   │   └── ConductorOperacionController.java ← campo conductor (PLAN-04)
    │   │   └── messaging/
    │   │       └── SolicitarRutaConsumer.java        ← Listener SQS
    │   └── out/
    │       ├── persistence/             ← impl de *RepositoryPort con JPA
    │       │   ├── RutaJpaAdapter.java
    │       │   ├── VehiculoJpaAdapter.java
    │       │   ├── ConductorJpaAdapter.java
    │       │   ├── ParadaJpaAdapter.java
    │       │   └── HistorialAsignacionJpaAdapter.java
    │       ├── messaging/               ← impl de IntegracionModulo1/3Port con SQS
    │       │   ├── SqsIntegracionModulo1Adapter.java
    │       │   └── SqsIntegracionModulo3Adapter.java
    │       ├── storage/
    │       │   └── S3AlmacenamientoAdapter.java      ← impl de AlmacenamientoArchivoPort
    │       └── notification/
    │           └── WebSocketNotificacionAdapter.java  ← impl de NotificacionDespachadorPort
    ├── persistence/
    │   ├── entity/                      ← Entidades JPA (separadas del dominio)
    │   │   ├── RutaEntity.java
    │   │   ├── VehiculoEntity.java
    │   │   ├── ConductorEntity.java
    │   │   ├── ParadaEntity.java
    │   │   └── HistorialAsignacionEntity.java
    │   └── repository/                  ← Spring Data JPA interfaces
    │       ├── RutaJpaRepository.java
    │       ├── VehiculoJpaRepository.java
    │       ├── ConductorJpaRepository.java
    │       ├── ParadaJpaRepository.java
    │       └── HistorialAsignacionJpaRepository.java
    ├── scheduler/
    │   ├── FechaLimiteDespachoScheduler.java   ← @Scheduled + @SchedulerLock
    │   └── CierreAutomaticoScheduler.java      ← @Scheduled + @SchedulerLock
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── AwsSqsConfig.java
    │   ├── SwaggerConfig.java
    │   └── GlobalExceptionHandler.java          ← mapea domain exceptions → HTTP
    └── dto/
        ├── request/                     ← Records inmutables (Java 17+)
        │   ├── SolicitarRutaRequest.java
        │   ├── ConfirmarDespachoRequest.java
        │   ├── VehiculoRequest.java
        │   ├── ConductorRequest.java
        │   ├── AsignacionRequest.java
        │   ├── RegistrarParadaRequest.java
        │   └── CierreRutaRequest.java
        └── response/
            ├── SolicitarRutaResponse.java
            ├── RutaDetalleResponse.java
            ├── RutaConductorResponse.java
            ├── VehiculoResponse.java
            ├── FlotaDisponibilidadResponse.java
            └── ErrorResponse.java
```

---

## Esquema de Base de Datos (Flyway V1__init.sql)

```sql
-- ENUMs PostgreSQL
CREATE TYPE estado_ruta AS ENUM (
    'CREADA', 'LISTA_PARA_DESPACHO', 'CONFIRMADA', 'EN_TRANSITO',
    'CERRADA_MANUAL', 'CERRADA_AUTOMATICA', 'CERRADA_FORZADA'
);
CREATE TYPE tipo_vehiculo  AS ENUM ('MOTO', 'VAN', 'NHR', 'TURBO');
CREATE TYPE estado_vehiculo AS ENUM ('DISPONIBLE', 'EN_TRANSITO', 'INACTIVO');
CREATE TYPE estado_conductor AS ENUM ('ACTIVO', 'INACTIVO', 'EN_RUTA');
CREATE TYPE estado_parada AS ENUM ('PENDIENTE','EXITOSA','FALLIDA','NOVEDAD','SIN_GESTION_CONDUCTOR','EXCLUIDA_DESPACHO');
CREATE TYPE motivo_novedad AS ENUM (
    'CLIENTE_AUSENTE','DIRECCION_INCORRECTA','ZONA_DIFICIL_ACCESO',
    'RECHAZADO_POR_CLIENTE','DAÑADO_EN_RUTA','EXTRAVIADO','DEVOLUCION'
);
CREATE TYPE origen_parada AS ENUM ('CONDUCTOR','SISTEMA');
CREATE TYPE tipo_cierre    AS ENUM ('MANUAL','AUTOMATICO','FORZADO_DESPACHADOR');

-- VEHICULOS
CREATE TABLE vehiculos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placa             VARCHAR(10)  NOT NULL UNIQUE,
    tipo              tipo_vehiculo NOT NULL,
    modelo            VARCHAR(100) NOT NULL,
    capacidad_peso_kg NUMERIC(10,2) NOT NULL CHECK (capacidad_peso_kg > 0),
    volumen_maximo_m3 NUMERIC(10,2) NOT NULL CHECK (volumen_maximo_m3 > 0),
    zona_operacion    VARCHAR(20)  NOT NULL,    -- geohash precisión 3 (zona amplia)
    estado            estado_vehiculo NOT NULL DEFAULT 'DISPONIBLE',
    conductor_id      UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vehiculos_tipo_estado ON vehiculos(tipo, estado);
CREATE INDEX idx_vehiculos_zona_estado ON vehiculos(zona_operacion, estado);

-- CONDUCTORES
CREATE TABLE conductores (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre               VARCHAR(200) NOT NULL,
    email                VARCHAR(255) UNIQUE,
    modelo_contrato      VARCHAR(50) NOT NULL,   -- RECORRIDO_COMPLETO | POR_PARADA
    estado               estado_conductor NOT NULL DEFAULT 'ACTIVO',
    vehiculo_asignado_id UUID REFERENCES vehiculos(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_conductores_estado ON conductores(estado);

-- FK circular conductor ↔ vehiculo (DEFERRABLE para evitar orden de inserción)
ALTER TABLE vehiculos
    ADD CONSTRAINT fk_vehiculos_conductor
    FOREIGN KEY (conductor_id) REFERENCES conductores(id)
    DEFERRABLE INITIALLY DEFERRED;

-- HISTORIAL DE ASIGNACIONES
CREATE TABLE historial_asignaciones (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conductor_id      UUID NOT NULL REFERENCES conductores(id),
    vehiculo_id       UUID NOT NULL REFERENCES vehiculos(id),
    fecha_hora_inicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_hora_fin    TIMESTAMPTZ            -- NULL = asignación activa
);
CREATE INDEX idx_historial_conductor ON historial_asignaciones(conductor_id);
-- Partial index: búsqueda rápida de asignación activa de un conductor
CREATE INDEX idx_historial_activo ON historial_asignaciones(conductor_id)
    WHERE fecha_hora_fin IS NULL;

-- RUTAS
CREATE TABLE rutas (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zona                    VARCHAR(20)   NOT NULL,   -- geohash precisión 5
    estado                  estado_ruta   NOT NULL DEFAULT 'CREADA',
    peso_acumulado_kg       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (peso_acumulado_kg >= 0),
    tipo_vehiculo_requerido tipo_vehiculo NOT NULL DEFAULT 'MOTO',
    vehiculo_id             UUID REFERENCES vehiculos(id),
    conductor_id            UUID REFERENCES conductores(id),
    fecha_creacion_ruta     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_limite_despacho   TIMESTAMPTZ NOT NULL,
    fecha_hora_inicio       TIMESTAMPTZ,
    fecha_hora_cierre       TIMESTAMPTZ,
    tipo_cierre             tipo_cierre,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- CLAVE: único parcial — garantiza una sola ruta CREADA por zona (control de concurrencia en BD)
CREATE UNIQUE INDEX idx_rutas_zona_creada ON rutas(zona) WHERE estado = 'CREADA';
CREATE INDEX idx_rutas_estado ON rutas(estado);
-- Para el scheduler de fecha límite
CREATE INDEX idx_rutas_fecha_limite ON rutas(fecha_limite_despacho) WHERE estado = 'CREADA';
-- Para el scheduler de cierre automático
CREATE INDEX idx_rutas_fecha_inicio ON rutas(fecha_hora_inicio) WHERE estado = 'EN_TRANSITO';
CREATE INDEX idx_rutas_conductor ON rutas(conductor_id);

-- PARADAS
CREATE TABLE paradas (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ruta_id              UUID NOT NULL REFERENCES rutas(id),
    paquete_id           UUID NOT NULL,             -- referencia externa al SGP
    orden                INTEGER NOT NULL DEFAULT 0,
    direccion            VARCHAR(500) NOT NULL,
    latitud              NUMERIC(10,8) NOT NULL,
    longitud             NUMERIC(11,8) NOT NULL,
    tipo_mercancia       VARCHAR(20),               -- FRAGIL | PELIGROSO | ESTANDAR
    metodo_pago          VARCHAR(20),               -- PREPAGO | CONTRA_ENTREGA
    fecha_limite_entrega TIMESTAMPTZ,
    estado               estado_parada NOT NULL DEFAULT 'PENDIENTE',
    motivo_novedad       motivo_novedad,
    fecha_hora_gestion   TIMESTAMPTZ,               -- timestamp real del conductor (offline)
    firma_receptor_url   VARCHAR(1000),
    foto_evidencia_url   VARCHAR(1000),
    nombre_receptor      VARCHAR(200),
    origen               origen_parada NOT NULL DEFAULT 'CONDUCTOR',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ruta_id, paquete_id)                    -- un paquete una sola vez por ruta
);
CREATE INDEX idx_paradas_ruta ON paradas(ruta_id);
CREATE INDEX idx_paradas_ruta_estado ON paradas(ruta_id, estado);
CREATE INDEX idx_paradas_paquete ON paradas(paquete_id);
```

---

## Puertos: Definición de Interfaces Clave

```java
// ─── DRIVING PORTS (domain/port/in) ─────────────────────────────────

interface PlanificacionRouteUseCase {
    UUID solicitarRuta(SolicitarRutaCommand command);
    void despacharManual(UUID rutaId);
    void transicionarRutasVencidas(); // llamado por FechaLimiteDespachoScheduler
}

interface DespachoRouteUseCase {
    List<Ruta> listarRutasParaDespacho();
    Ruta confirmarDespacho(UUID rutaId, ConfirmarDespachoCommand command);
    void excluirPaquete(UUID rutaId, UUID paqueteId, String motivo);
}

interface GestionFlotaUseCase {
    Vehiculo registrar(RegistrarVehiculoCommand command);
    Vehiculo actualizar(UUID id, ActualizarVehiculoCommand command);
    void darDeBaja(UUID id);
    List<Vehiculo> consultarDisponibilidad();
}

interface GestionConductoresUseCase {
    Conductor registrar(RegistrarConductorCommand command);
    void asignarVehiculo(UUID conductorId, UUID vehiculoId);
    void desvincularVehiculo(UUID conductorId);
    void darDeBaja(UUID conductorId);
    List<HistorialAsignacion> consultarHistorial(UUID conductorId);
}

interface OperacionCampoUseCase {
    RutaConductorView consultarRutaActiva(UUID conductorId);
    void iniciarTransito(UUID rutaId, UUID conductorId);
    void registrarResultadoParada(UUID rutaId, RegistrarParadaCommand command);
    void cerrarRuta(UUID rutaId, boolean confirmarConPendientes);
    void forzarCierre(UUID rutaId);
    void cerrarRutasExcedidas(); // llamado por CierreAutomaticoScheduler
}

// ─── DRIVEN PORTS (domain/port/out) ─────────────────────────────────

interface IntegracionModulo1Port {
    void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHoraEvento);
    void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fecha, String urlFoto, String urlFirma);
    void publishParadaFallida(UUID paqueteId, UUID rutaId, Instant fecha, MotivoNovedad motivo);
    void publishNovedadGrave(UUID paqueteId, UUID rutaId, Instant fecha, MotivoNovedad tipoNovedad);
    void publishParadasSinGestionar(UUID rutaId, TipoCierre tipoCierre, List<UUID> paqueteIds);
    void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, Instant fecha);
}

interface IntegracionModulo3Port {
    void publishRutaCerrada(RutaCerradaEvent event); // payload según SPEC-08 sección 4
}

interface AlmacenamientoArchivoPort {
    String almacenarFoto(UUID paradaId, byte[] foto, String contentType);
    String almacenarFirma(UUID paradaId, byte[] firma, String contentType);
}

interface NotificacionDespachadorPort {
    void notificarRutaListaParaDespacho(UUID rutaId, String zona, int numPaquetes,
                                        double pesoKg, TipoVehiculo tipo, String motivo);
    void notificarAlertaPrioritaria(String mensaje);
}
```

---

## Fase 0 — Fundación (Sprint 0) · PREREQUISITO DE TODO

> Antes de implementar cualquier historia de usuario, esta fase debe estar completa.

### F0.1 — Setup del proyecto
- [ ] T000 Crear proyecto Spring Boot 3.x + Java 21
- [ ] T001 Dependencias: `web`, `data-jpa`, `security`, `validation`, `postgresql`, `lombok`, `flyway-core`, `spring-cloud-aws-sqs-starter`, `geohash-java`, `shedlock-spring`, `shedlock-provider-redis-spring`
- [ ] T002 Crear estructura hexagonal de packages (carpetas con `.gitkeep`)
- [ ] T003 Configurar `application.yml` con perfiles `dev`, `test`, `prod`

### F0.2 — Base de datos
- [ ] T004 Crear `V1__init.sql` con el script completo del esquema anterior (Flyway)
- [ ] T005 Crear `V2__seed.sql` con `parametros_sistema` y datos de prueba
- [ ] T006 Verificar que Flyway migra correctamente en el startup con `validate-on-migrate=true`

### F0.3 — Dominio puro
- [ ] T007 Crear todos los enums de dominio (incluyendo `TipoVehiculo` con `siguienteTipo()` y `capacidadKg()`)
- [ ] T008 Crear `ZonaGeografica` record inmutable con `GeoHash.geoHashStringWithCharacterPrecision(lat, lon, 5)`
- [ ] T009 Crear entidades de dominio como POJOs (sin anotaciones JPA): `Ruta`, `Vehiculo`, `Conductor`, `Parada`, `HistorialAsignacion`
- [ ] T010 Crear todas las interfaces de puertos (in/out) vacías pero compilables
- [ ] T011 Crear jerarquía de excepciones de dominio

### F0.4 — Capa de persistencia
- [ ] T012 Crear entidades JPA (`*Entity`) separadas de las entidades de dominio
- [ ] T013 Crear interfaces `*JpaRepository extends JpaRepository`
- [ ] T014 Crear adaptadores JPA: `*JpaAdapter implements *RepositoryPort` con mappers dominio ↔ entity

### F0.5 — Seguridad
- [ ] T015 Configurar Spring Security con JWT (3 roles: `DISPATCHER`, `DRIVER`, `FLEET_ADMIN`, `SYSTEM`)
- [ ] T016 `POST /api/auth/login` — endpoint de autenticación

### F0.6 — Concerns transversales
- [ ] T017 `GlobalExceptionHandler (@RestControllerAdvice)` que mapea domain exceptions → HTTP 4xx
- [ ] T018 OpenAPI/Swagger con `SwaggerConfig`
- [ ] T019 Logs estructurados con MDC (`ruta_id`, `conductor_id`) para trazabilidad

**⛔ CHECKPOINT 0: Proyecto compila, BD migra, seguridad activa, estructura hexagonal lista.**

---

## Orden Total de Ejecución

```
Sprint 0 — Fundación (PLAN-00)      ← Este archivo
    └── Sprint 1 — Gestión de Flota (PLAN-03)
            └── Sprint 2 — Planificación de Rutas (PLAN-01)
                    └── Sprint 3 — Despacho (PLAN-02)
                            └── Sprint 4 — Operación de Campo (PLAN-04)
                                    ├── Sprint 5-6 — Frontend React (docs pendientes)
                                    └── Sprint 7 — AWS + CI/CD (docs pendientes)
```

