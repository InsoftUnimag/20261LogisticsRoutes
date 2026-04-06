# Implementation Plan: Operación de Campo del Conductor (MOD2-UC-006 / MOD2-UC-007)

**Date:** 2026-04-06
**Specs:**
- [SPEC-06-consulta-ruta-conductor.md](../specs/SPEC-06-consulta-ruta-conductor.md)
- [SPEC-07-operacion-campo-conductor.md](../specs/SPEC-07-operacion-campo-conductor.md)

---

## Summary

Este plan cubre la experiencia completa del conductor en campo. Incluye: consulta de la ruta asignada con sus paradas ordenadas, inicio del tránsito (transición a `EN_TRANSITO` y notificación a Módulo 1), registro del resultado de cada parada (exitosa, fallida, novedad grave con eventos al Módulo 1), cierre manual de ruta y generación del evento `RUTA_CERRADA` al Módulo 3, cierre automático por exceder 2 días en tránsito (scheduler), y cierre forzado por el Despachador. Toda operación debe funcionar con soporte offline básico.

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
| **Performance Goals** | Evento `PAQUETE_EN_TRANSITO` a Módulo 1 en < 5 segundos. Evento `RUTA_CERRADA` a Módulo 3 en < 5 segundos. |
| **Constraints** | POD (foto) obligatorio para parada exitosa. Timestamps corresponden al momento real de la acción del conductor, no al de sincronización. Conductor no interactúa directamente con Módulo 3. |
| **Scale/Scope** | Módulo 2 — endpoints de operación de campo + scheduler de cierre automático |

---

## Project Structure

> Este plan extiende la estructura de los planes anteriores. La entidad `Parada` es nueva en este plan.

```
src/
├── main/
│   └── java/com/logistics/routes/
│       ├── model/
│       │   ├── Parada.java                    [NUEVO]
│       │   └── EstadoParada.java              [NUEVO — enum]
│       ├── repository/
│       │   └── ParadaRepository.java          [NUEVO]
│       ├── service/
│       │   ├── ConductorOperacionService.java [NUEVO]
│       │   ├── CierreRutaService.java         [NUEVO]
│       │   └── IntegracionExternaService.java [NUEVO — stub M1 y M3]
│       ├── scheduler/
│       │   └── CierreAutomaticoScheduler.java [NUEVO]
│       ├── controller/
│       │   ├── ConductorController.java       [NUEVO]
│       │   └── DespachoController.java        (ya existe — se extiende con cierre forzado)
│       └── dto/
│           ├── RutaConductorResponse.java     [NUEVO]
│           ├── RegistrarParadaRequest.java    [NUEVO]
│           └── CierreRutaRequest.java         [NUEVO]
└── test/
    └── java/com/logistics/routes/
        ├── service/
        │   ├── ConductorOperacionServiceTest.java [NUEVO]
        │   └── CierreRutaServiceTest.java         [NUEVO]
        ├── scheduler/
        │   └── CierreAutomaticoSchedulerTest.java [NUEVO]
        └── integration/
            └── OperacionCampoIntegrationTest.java [NUEVO]
```

**Structure Decision:** Se extiende el mismo proyecto Spring Boot. La entidad `Parada` es propia de este módulo y se almacena en la BD local. Los eventos a Módulo 1 y Módulo 3 van al `IntegracionExternaService` (stub que loggea el payload; se conectará al canal real posteriormente).

---

## Phase 1: Setup

> **Dependencia:** PLAN-01 Phase 2, PLAN-02 Phase 4 (rutas en estado `CONFIRMADA`) deben estar completas.

- [ ] T001 Verificar que existen rutas en estado `CONFIRMADA` con conductor y vehículo asignados

---

## Phase 2: Foundational — Entidad Parada

**Purpose:** Modelo `Parada` y su repositorio. Sin esto no se pueden registrar resultados de paradas ni cerrar rutas.

⚠️ **CRÍTICO: Ninguna historia de operación de campo puede iniciarse sin esta fase.**

- [ ] T002 [P] Crear enum `EstadoParada`: `PENDIENTE`, `EXITOSA`, `FALLIDA`, `NOVEDAD`, `SIN_GESTION_CONDUCTOR`
- [ ] T003 [P] Crear enum `MotivoNovedad`: `CLIENTE_AUSENTE`, `DIRECCION_INCORRECTA`, `ZONA_DIFICIL_ACCESO`, `RECHAZADO_POR_CLIENTE`, `DAÑADO_EN_RUTA`, `EXTRAVIADO`, `DEVOLUCION`
- [ ] T004 [P] Crear enum `OrigenParada`: `CONDUCTOR`, `SISTEMA`
- [ ] T005 [P] Crear entidad `Parada` en `model/Parada.java` con todos los atributos de KEY-ENTITIES: `paqueteId` (UUID), `rutaId` (UUID), `direccion` (String), `latitud` (float), `longitud` (float), `estado` (EstadoParada), `motivoNovedad` (MotivoNovedad, nullable), `fechaHoraGestion` (LocalDateTime, nullable), `firmaReceptorUrl` (String, nullable), `fotoEvidenciaUrl` (String, nullable), `nombreReceptor` (String, nullable), `origen` (OrigenParada)
- [ ] T006 [P] Crear `ParadaRepository` con queries:
  - `findByRutaId(UUID rutaId)` — todas las paradas de una ruta
  - `findByRutaIdAndEstado(UUID rutaId, EstadoParada estado)` — paradas pendientes
- [ ] T007 [P] Crear stub `IntegracionExternaService` con métodos:
  - `notificarPaqueteEnTransito(UUID paqueteId, UUID rutaId, LocalDateTime fechaHoraEvento)`
  - `notificarPaqueteEntregado(UUID paqueteId, UUID rutaId, LocalDateTime fecha, String urlFoto, String urlFirma)`
  - `notificarParadaFallida(UUID paqueteId, UUID rutaId, LocalDateTime fecha, String motivo)`
  - `notificarNovedadGrave(UUID paqueteId, UUID rutaId, LocalDateTime fecha, String tipoNovedad)`
  - `notificarParadasSinGestionar(UUID rutaId, String tipoCierre, List<UUID> paqueteIds)`
  - `notificarRutaCerrada(Ruta ruta, List<Parada> paradas)` — payload completo a Módulo 3

**Checkpoint: Entidad Parada e IntegracionExternaService listos. Se puede iniciar implementación de campo.**

---

## Phase 3: User Story 6 — Consulta de Ruta por el Conductor (Priority: P2)

**Goal:** El conductor consulta su ruta asignada con paradas ordenadas y puede iniciar el tránsito.

**Independent Test:** Con una ruta en `CONFIRMADA`, llamar `GET /api/conductor/ruta-activa` → verificar que retorna paradas ordenadas. Llamar `POST /api/conductor/ruta/{id}/iniciar-transito` → verificar transición a `EN_TRANSITO` y que `IntegracionExternaService` recibe el evento `PAQUETE_EN_TRANSITO` para cada paquete.

### Tests para US6
- [ ] T008 [P] [US6] Test unitario: `ConductorOperacionServiceTest` — consulta retorna ruta con paradas ordenadas en estado `CONFIRMADA`
- [ ] T009 [P] [US6] Test unitario: consulta sin ruta asignada retorna respuesta vacía con mensaje
- [ ] T010 [P] [US6] Test unitario: iniciar tránsito transiciona ruta a `EN_TRANSITO` y registra `fechaHoraInicio`
- [ ] T011 [US6] Test unitario: iniciar tránsito llama a `IntegracionExternaService.notificarPaqueteEnTransito()` por cada paquete de la ruta

### Implementación para US6
- [ ] T012 [P] [US6] Implementar `ConductorOperacionService.consultarRutaActiva(UUID conductorId)`:
  - Buscar ruta en estado `CONFIRMADA` con el `conductorId` dado
  - Retornar `RutaConductorResponse` con paradas ordenadas (orden optimizado del despacho), detalle de cada paquete
  - Si no hay ruta → retornar respuesta vacía con mensaje
- [ ] T013 [P] [US6] Implementar `ConductorOperacionService.iniciarTransito(UUID rutaId)`:
  - Verificar ruta en estado `CONFIRMADA`
  - Transicionar a `EN_TRANSITO`, registrar `fechaHoraInicio = now()`
  - Por cada `paqueteId` en las paradas: llamar `IntegracionExternaService.notificarPaqueteEnTransito()`
- [ ] T014 [US6] Implementar `ConductorController`:
  - `GET /api/conductor/ruta-activa` — consultar ruta activa
  - `POST /api/conductor/rutas/{id}/iniciar-transito` — iniciar tránsito

**Checkpoint: El conductor puede ver su ruta y iniciarla en tránsito. Módulo 1 recibe notificación.**

---

## Phase 4: User Story 7a — Registrar Resultado de Parada (Priority: P2)

**Goal:** El conductor registra el resultado de cada parada: exitosa (con POD), fallida (con motivo) o novedad grave. Cada registro notifica al Módulo 1.

**Independent Test:** Con ruta en `EN_TRANSITO`, registrar parada exitosa con foto URL → verificar estado `EXITOSA` y llamada a `notificarPaqueteEntregado`. Registrar parada fallida con motivo → verificar estado `FALLIDA` y llamada a `notificarParadaFallida`.

### Tests para US7a
- [ ] T015 [P] [US7a] Test unitario: `ConductorOperacionServiceTest` — parada exitosa con POD cambia estado a `EXITOSA`
- [ ] T016 [P] [US7a] Test unitario: parada exitosa sin foto POD es rechazada con error de validación
- [ ] T017 [P] [US7a] Test unitario: parada fallida con motivo cambia estado a `FALLIDA`
- [ ] T018 [P] [US7a] Test unitario: novedad grave cambia estado a `NOVEDAD`
- [ ] T019 [US7a] Test unitario: cada registro llama al método correspondiente de `IntegracionExternaService`
- [ ] T020 [US7a] Test de integración: `OperacionCampoIntegrationTest` — flujo completo de paradas en ruta en tránsito

### Implementación para US7a
- [ ] T021 [P] [US7a] Crear DTO `RegistrarParadaRequest`: `paqueteId`, `resultado` (EXITOSA / FALLIDA / NOVEDAD), `motivoNovedad` (nullable), `urlFoto` (nullable), `urlFirma` (nullable), `nombreReceptor` (nullable), `fechaHoraAccion` (ISO8601 — timestamp del conductor)
- [ ] T022 [P] [US7a] Implementar `ConductorOperacionService.registrarResultadoParada(UUID rutaId, RegistrarParadaRequest)`:
  - Verificar que la ruta está `EN_TRANSITO`
  - Si resultado = `EXITOSA`: validar que `urlFoto` no es null → actualizar parada, llamar `notificarPaqueteEntregado()`
  - Si resultado = `FALLIDA`: validar que `motivoNovedad` no es null → actualizar parada, llamar `notificarParadaFallida()`
  - Si resultado = `NOVEDAD`: validar tipo de novedad → actualizar parada, llamar `notificarNovedadGrave()`
  - Usar `fechaHoraAccion` del request como timestamp (no `now()`) para soporte offline
- [ ] T023 [US7a] Agregar endpoint en `ConductorController`:
  - `POST /api/conductor/rutas/{id}/paradas/{paqueteId}/resultado` — registrar resultado de parada

**Checkpoint: El conductor puede gestionar todas las paradas. Módulo 1 se actualiza en tiempo real.**

---

## Phase 5: User Story 7b — Cierre de Ruta (Priority: P1)

**Goal:** El conductor cierra la ruta una vez finalizadas las paradas. El sistema genera y envía el evento `RUTA_CERRADA` al Módulo 3. Si hay paradas pendientes, advierte y da opción. El Despachador puede forzar el cierre.

**Independent Test:** Gestionar todas las paradas de una ruta y llamar `POST /api/conductor/rutas/{id}/cerrar` → verificar transición a `CERRADA_MANUAL` y llamada a `notificarRutaCerrada()`. Intentar cerrar con paradas pendientes → verificar advertencia. Forzar cierre desde Despachador → verificar `CERRADA_FORZADA` y paradas en `SIN_GESTION_CONDUCTOR`.

### Tests para US7b — Cierre manual
- [ ] T024 [P] [US7b] Test unitario: `CierreRutaServiceTest` — cierre exitoso con todas las paradas gestionadas
- [ ] T025 [P] [US7b] Test unitario: cierre con paradas pendientes retorna advertencia con lista de pendientes
- [ ] T026 [P] [US7b] Test unitario: confirmar cierre con pendientes marca paradas como `SIN_GESTION_CONDUCTOR` con origen `SISTEMA`
- [ ] T027 [US7b] Test unitario: cierre llama a `IntegracionExternaService.notificarRutaCerrada()` con payload completo según SPEC-08
- [ ] T028 [US7b] Test unitario: cierre forzado por Despachador registra `tipo_cierre: FORZADO_DESPACHADOR`

### Tests para US7b — Cierre automático
- [ ] T029 [P] [US7b] Test unitario: `CierreAutomaticoSchedulerTest` — rutas que superan 2 días `EN_TRANSITO` son cerradas automáticamente
- [ ] T030 [US7b] Test unitario: cierre automático marca pendientes como `SIN_GESTION_CONDUCTOR` y llama `notificarParadasSinGestionar()` y `notificarRutaCerrada()`

### Implementación para US7b
- [ ] T031 [P] [US7b] Implementar `CierreRutaService.cerrarRuta(UUID rutaId, boolean confirmarConPendientes, String tipoCierre)`:
  - Verificar que la ruta está `EN_TRANSITO`
  - Obtener paradas pendientes
  - Si hay pendientes y `!confirmarConPendientes` → retornar advertencia con lista de pendientes
  - Si `confirmarConPendientes` o no hay pendientes:
    - Marcar paradas pendientes como `SIN_GESTION_CONDUCTOR`, origen `SISTEMA`
    - Transicionar ruta a `tipoCierre` (`CERRADA_MANUAL` / `CERRADA_FORZADA`)
    - Si hay pendientes: llamar `notificarParadasSinGestionar()`
    - Llamar `notificarRutaCerrada()` con payload completo del SPEC-08
- [ ] T032 [US7b] Agregar endpoint en `ConductorController`:
  - `POST /api/conductor/rutas/{id}/cerrar` — cierre manual (body: `CierreRutaRequest` con `confirmarConPendientes`)
- [ ] T033 [US7b] Agregar endpoint en `DespachoController`:
  - `POST /api/despacho/rutas/{id}/forzar-cierre` — cierre forzado por Despachador
- [ ] T034 [P] [US7b] Implementar `CierreAutomaticoScheduler` con `@Scheduled(fixedRate = 3600000)` (cada hora):
  - Query: rutas en `EN_TRANSITO` con `fechaHoraInicio <= now() - 2 días`
  - Para cada una: llamar `CierreRutaService.cerrarRuta(rutaId, true, "AUTOMATICO")`
  - Enviar alerta de alta prioridad al Despachador
- [ ] T035 [US7b] Agregar query en `RutaRepository`: `findByEstadoAndFechaHoraInicioLessThanEqual(EstadoRuta estado, LocalDateTime fecha)`

**Checkpoint: El flujo de cierre completo funciona — manual, forzado y automático — con eventos correctos al Módulo 3.**

---

## Phase N: Polish y Concerns Transversales

- [ ] T036 Documentar todos los endpoints de operación de campo con Javadoc y contratos de SPEC-08
- [ ] T037 Revisar logs estructurados en `IntegracionExternaService` (payload completo en nivel DEBUG)
- [ ] T038 Agregar validaciones `@Valid` en `RegistrarParadaRequest` y `CierreRutaRequest`
- [ ] T039 Revisar manejo de soporte offline: verificar que el campo `fechaHoraAccion` del request se usa correctamente en todos los registros
- [ ] T040 Agregar test de integración E2E: flujo completo conductor — iniciar tránsito → registrar paradas → cerrar ruta → verificar payload a Módulo 3

---

## Dependencies & Execution Order

```
PLAN-01 Phase 2 (modelo Ruta)
PLAN-02 Phase 4 (rutas en estado CONFIRMADA, Conductor y Vehiculo asignados)
    └── Este plan — Phase 1 (verificación)
            └── Phase 2 (Foundational Parada e IntegracionExternaService — BLOQUEA todo)
                    ├── Phase 3 (US6: Consulta y inicio de tránsito) — comenzar aquí
                    ├── Phase 4 (US7a: Registro de paradas)          — depende de Phase 3
                    └── Phase 5 (US7b: Cierre de ruta)               — depende de Phase 4

Phase N (Polish) — al final
```

---

## Notes

- `IntegracionExternaService` es un stub de logs estructurados en este plan. Cuando el equipo defina el canal real de comunicación con Módulo 1 y Módulo 3 (REST callback, mensajería asíncrona, etc.), solo se modifica esta clase sin impactar el resto del código.
- El soporte offline completo (almacenamiento local en dispositivo del conductor) es responsabilidad del frontend/app móvil. El backend usa el `fechaHoraAccion` del request para preservar los timestamps correctos.
- El payload de `notificarRutaCerrada()` debe seguir **exactamente** la estructura definida en SPEC-08 sección 4.
- Verificar tests y hacer commit al terminar cada tarea o grupo lógico.
- Detener en cada Checkpoint para validar independientemente antes de continuar.
