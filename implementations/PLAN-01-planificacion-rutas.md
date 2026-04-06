# Implementation Plan: Planificación Automática de Rutas (MOD2-UC-001)

**Date:** 2026-04-06
**Spec:** [SPEC-01-planificacion-rutas.md](../specs/SPEC-01-planificacion-rutas.md)

---

## Summary

El sistema recibe eventos `SOLICITAR_RUTA` del Módulo 1 (SGP) cuando un paquete alcanza estado `recibido_en_sede`. Agrupa paquetes por zona geográfica (radio 5 km), asigna el tipo de vehículo de menor capacidad disponible, recalcula y escala el tipo de vehículo al superar el 90% de capacidad, transiciona rutas a `lista_para_despacho` por capacidad o por vencimiento de plazo (5 días), y notifica al Despachador Logístico. La comunicación con Módulo 1 es asíncrona.

---

## Technical Context

| Campo | Valor |
|---|---|
| **Language/Version** | Java 17 |
| **Framework** | Spring Boot 3.x |
| **Primary Dependencies** | Spring Web, Spring Data JPA, Spring Scheduler, PostgreSQL Driver, Lombok |
| **Storage** | PostgreSQL |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Target Platform** | Linux server / Backend REST API |
| **Performance Goals** | Retornar `ruta_id` al SGP en < 2 segundos; transición a `lista_para_despacho` notificada en < 5 segundos |
| **Constraints** | Sin duplicados de ruta por zona en estado `CREADA` (control de concurrencia); descartar solicitudes con `fecha_limite_entrega` vencida |
| **Scale/Scope** | Módulo 2 del sistema logístico — endpoint y scheduler de planificación de rutas |

---

## Project Structure

```
src/
├── main/
│   └── java/com/logistics/routes/
│       ├── model/
│       │   ├── Ruta.java
│       │   └── TipoVehiculo.java
│       ├── repository/
│       │   └── RutaRepository.java
│       ├── service/
│       │   ├── PlanificacionService.java
│       │   └── NotificacionService.java
│       ├── scheduler/
│       │   └── FechaLimiteScheduler.java
│       ├── controller/
│       │   └── PlanificacionController.java
│       └── dto/
│           ├── SolicitarRutaRequest.java
│           └── SolicitarRutaResponse.java
└── test/
    └── java/com/logistics/routes/
        ├── service/
        │   └── PlanificacionServiceTest.java
        └── integration/
            └── PlanificacionIntegrationTest.java
```

**Structure Decision:** Proyecto single backend (Spring Boot). El frontend del despachador es parte de un plan separado (PLAN-02). Esta estructura expone un endpoint REST para recibir el evento del Módulo 1 y un scheduler para el chequeo de fechas límite.

---

## Phase 1: Setup (Infraestructura compartida)

**Purpose:** Inicialización y configuración base del proyecto Spring Boot.

- [ ] T001 Crear proyecto Spring Boot con dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `lombok`, `spring-boot-starter-test`
- [ ] T002 Configurar `application.properties` con conexión a PostgreSQL (datasource, Hibernate DDL)
- [ ] T003 Crear esquema inicial de base de datos (script SQL o Flyway migration `V1__init.sql`)
- [ ] T004 Configurar Lombok y verificar compilación exitosa del proyecto vacío

---

## Phase 2: Foundational — Modelos y Repositorios (Prerequisito bloqueante)

**Purpose:** Entidades de dominio y acceso a datos. Nada puede implementarse sin esto.

⚠️ **CRÍTICO: Ninguna historia de usuario puede iniciarse hasta completar esta fase.**

- [ ] T005 [P] Crear enum `EstadoRuta` con valores: `CREADA`, `LISTA_PARA_DESPACHO`, `CONFIRMADA`, `EN_TRANSITO`, `CERRADA_MANUAL`, `CERRADA_AUTOMATICA`, `CERRADA_FORZADA`
- [ ] T006 [P] Crear enum `TipoVehiculo` con valores y capacidades: `MOTO(20)`, `VAN(500)`, `NHR(2000)`, `TURBO(4500)` — incluir método `siguiente()` para escalado
- [ ] T007 [P] Crear entidad `Ruta` en `model/Ruta.java` con todos los atributos de KEY-ENTITIES: `id` (UUID), `zona` (String), `estado` (EstadoRuta), `pesoAcumuladoKg` (float), `tipoVehiculoRequerido` (TipoVehiculo), `fechaCreacionRuta` (LocalDateTime), `fechaLimiteDespacho` (LocalDateTime), `paradas` (lista de IDs UUID)
- [ ] T008 [P] Crear `RutaRepository` extendiendo `JpaRepository<Ruta, UUID>` con query: `findByZonaAndEstado(String zona, EstadoRuta estado)`
- [ ] T009 [P] Crear DTOs: `SolicitarRutaRequest` (campos del evento SOLICITAR_RUTA del SPEC-08) y `SolicitarRutaResponse` con `ruta_id`

**Checkpoint: Base de datos y modelos listos. Se puede iniciar implementación de User Stories.**

---

## Phase 3: User Story 1 — Planificar y Asignar Rutas (Priority: P1)

**Goal:** Recibir el evento `SOLICITAR_RUTA`, agrupar el paquete en una ruta por zona, escalar el tipo de vehículo si es necesario y retornar `ruta_id`.

**Independent Test:** Enviar `POST /api/planificacion/solicitar-ruta` con un payload válido y verificar que se retorna un `ruta_id` y la ruta existe en la BD con el estado y tipo de vehículo correctos.

### Tests para User Story 1
- [ ] T010 [P] [US1] Test unitario: `PlanificacionServiceTest` — escenario "paquete asignado a ruta existente con zona coincidente"
- [ ] T011 [P] [US1] Test unitario: `PlanificacionServiceTest` — escenario "creación de nueva ruta cuando no existe ruta CREADA en la zona"
- [ ] T012 [P] [US1] Test unitario: `PlanificacionServiceTest` — escenario "escalar tipo de vehículo al superar 90% de capacidad"
- [ ] T013 [P] [US1] Test unitario: `PlanificacionServiceTest` — escenario "ruta pasa a LISTA_PARA_DESPACHO al superar 90% de capacidad del Turbo"
- [ ] T014 [US1] Test de integración: `PlanificacionIntegrationTest` — flujo completo con BD PostgreSQL en Testcontainers

### Implementación para User Story 1
- [ ] T015 [P] [US1] Implementar `PlanificacionService.asignarRuta(SolicitarRutaRequest)`:
  - Validar que `fecha_limite_entrega` no esté vencida; si venció, descartar y notificar al Despachador
  - Buscar ruta en estado `CREADA` para la misma zona (radio 5 km)
  - Si existe: agregar `paquete_id` a la ruta, sumar `peso_kg`, recalcular porcentaje de capacidad
  - Si no existe: crear nueva ruta con `tipoVehiculoRequerido = MOTO`, calcular `fechaLimiteDespacho = now() + 5 días`
  - Si el peso acumulado ≥ 90% de la capacidad del tipo actual:
    - Si existe tipo siguiente: escalar `tipoVehiculoRequerido` al siguiente
    - Si ya es TURBO: transicionar a `LISTA_PARA_DESPACHO` y llamar `NotificacionService.notificarDespachador(ruta)`
  - Retornar `ruta_id`
- [ ] T016 [US1] Implementar `PlanificacionController.solicitarRuta()` — `POST /api/planificacion/solicitar-ruta`
- [ ] T017 [US1] Agregar control de concurrencia en la creación de ruta por zona (`@Lock` o `SELECT FOR UPDATE` en repository) para evitar rutas duplicadas por zona
- [ ] T018 [US1] Implementar stub de `NotificacionService.notificarDespachador(Ruta ruta)` — log estructurado como placeholder
- [ ] T019 [US1] Agregar manejo de errores: `@ControllerAdvice` con respuestas 400/422 para solicitudes inválidas

**Checkpoint: US1 completamente funcional. El SGP puede enviar solicitudes y recibir `ruta_id`.**

---

## Phase 4: User Story 4 (Scheduler) — Transición automática por fecha límite (Priority: P1)

**Goal:** Detectar rutas en estado `CREADA` que alcanzaron su `fechaLimiteDespacho` y transicionarlas automáticamente a `LISTA_PARA_DESPACHO`.

**Independent Test:** Crear una ruta con `fechaLimiteDespacho` en el pasado, ejecutar el scheduler manualmente y verificar que la ruta transicionó a `LISTA_PARA_DESPACHO` y se notificó al Despachador con indicación de "vencimiento de plazo".

### Tests para el Scheduler
- [ ] T020 [P] [US1] Test unitario: `FechaLimiteSchedulerTest` — verifica que rutas con `fechaLimiteDespacho` vencida se transicionan correctamente
- [ ] T021 [US1] Test unitario: verifica que el mensaje de notificación al Despachador incluye "vencimiento de plazo"

### Implementación del Scheduler
- [ ] T022 [P] [US1] Agregar query en `RutaRepository`: `findByEstadoAndFechaLimiteDespachoLessThanEqual(EstadoRuta estado, LocalDateTime fecha)`
- [ ] T023 [US1] Implementar `FechaLimiteScheduler` con `@Scheduled(fixedRate = 60000)`:
  - Consultar rutas en estado `CREADA` con `fechaLimiteDespacho <= now()`
  - Transicionar cada una a `LISTA_PARA_DESPACHO`
  - Notificar al Despachador con motivo "vencimiento de plazo"
- [ ] T024 [US1] Habilitar scheduling en la aplicación con `@EnableScheduling`

**Checkpoint: Las rutas transicionan automáticamente por plazo vencido sin intervención manual.**

---

## Phase 5: User Story 5 — Despacho manual anticipado (Priority: P1)

**Goal:** Permitir al Despachador transicionar manualmente una ruta de `CREADA` a `LISTA_PARA_DESPACHO`.

**Independent Test:** Llamar `POST /api/planificacion/rutas/{id}/despacho-manual` sobre una ruta en estado `CREADA` y verificar que pasa a `LISTA_PARA_DESPACHO`.

### Tests para US5
- [ ] T025 [P] [US1] Test unitario: `PlanificacionServiceTest` — despacho manual exitoso
- [ ] T026 [US1] Test unitario: intento de despacho manual sobre ruta en estado distinto a `CREADA` retorna error

### Implementación de US5
- [ ] T027 [US1] Implementar `PlanificacionService.despachoManual(UUID rutaId)`
- [ ] T028 [US1] Implementar `PlanificacionController.despachoManual()` — `POST /api/planificacion/rutas/{id}/despacho-manual`

**Checkpoint: Todas las historias de US1 son funcionales y testables independientemente.**

---

## Phase N: Polish y Concerns Transversales

- [ ] T029 Documentar endpoints con Javadoc y comentarios de contrato
- [ ] T030 Revisar y ajustar logs estructurados (nivel INFO para eventos clave, ERROR para excepciones)
- [ ] T031 Agregar validaciones de entrada con `@Valid` y Bean Validation en los DTOs
- [ ] T032 Configurar gestión de variables de entorno sensibles (URL de BD, credenciales) vía `application.properties` con perfil `dev`/`prod`

---

## Dependencies & Execution Order

```
Phase 1 (Setup)
    └── Phase 2 (Foundational — BLOQUEA todo)
            ├── Phase 3 (US1: Asignación de rutas) — P1
            ├── Phase 4 (Scheduler: fecha límite)  — depende de Phase 3
            └── Phase 5 (Despacho manual)           — puede ir en paralelo con Phase 3

Phase N (Polish) — después de todas las fases anteriores
```

---

## Notes

- El campo `zona` en `Ruta` representa la zona geográfica agrupada por radio de 5 km. La estrategia de cálculo de zona (e.g., geohash o cluster por coordenadas) debe definirse antes de T015.
- `NotificacionService` es un stub en este plan; se conectará al canal real de notificación (WebSocket, email, etc.) en un plan posterior.
- Verificar tests y hacer commit al terminar cada tarea o grupo lógico.
- Detener en cada Checkpoint para validar de forma independiente.
