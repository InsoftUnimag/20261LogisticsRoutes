-- =============================================================================
-- V1__init.sql  —  Schema inicial del Sistema de Gestión de Rutas Logísticas
-- Módulo 2: Planificación y Gestión de Rutas
-- =============================================================================

-- =============================================================================
-- A. TIPOS ENUM
-- =============================================================================

CREATE TYPE estado_ruta AS ENUM (
    'CREADA',
    'LISTA_PARA_DESPACHO',
    'CONFIRMADA',
    'EN_TRANSITO',
    'CERRADA_MANUAL',
    'CERRADA_AUTOMATICA',
    'CERRADA_FORZADA'
);

CREATE TYPE tipo_vehiculo AS ENUM (
    'MOTO',
    'VAN',
    'NHR',
    'TURBO'
);

CREATE TYPE estado_vehiculo AS ENUM (
    'DISPONIBLE',
    'EN_TRANSITO',
    'INACTIVO'
);

CREATE TYPE estado_conductor AS ENUM (
    'ACTIVO',
    'INACTIVO',
    'EN_RUTA'
);

CREATE TYPE estado_parada AS ENUM (
    'PENDIENTE',
    'EXITOSA',
    'FALLIDA',
    'NOVEDAD',
    'SIN_GESTION_CONDUCTOR',
    'EXCLUIDA_DESPACHO'
);

CREATE TYPE motivo_novedad AS ENUM (
    'CLIENTE_AUSENTE',
    'DIRECCION_INCORRECTA',
    'ZONA_DIFICIL_ACCESO',
    'RECHAZADO_POR_CLIENTE',
    'DAÑADO_EN_RUTA',
    'EXTRAVIADO',
    'DEVOLUCION'
);

CREATE TYPE origen_parada AS ENUM (
    'CONDUCTOR',
    'SISTEMA'
);

CREATE TYPE tipo_cierre AS ENUM (
    'MANUAL',
    'AUTOMATICO',
    'FORZADO_DESPACHADOR'
);

CREATE TYPE modelo_contrato AS ENUM (
    'RECORRIDO_COMPLETO',
    'POR_PARADA'
);

-- =============================================================================
-- B. TABLA vehiculos
-- conductor_id se agrega sin FK aquí; la FK circular se agrega después
-- con DEFERRABLE INITIALLY DEFERRED para evitar conflictos de orden de inserción.
-- =============================================================================

CREATE TABLE vehiculos (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    placa             VARCHAR(10)  NOT NULL UNIQUE,
    tipo              tipo_vehiculo NOT NULL,
    modelo            VARCHAR(100) NOT NULL,
    capacidad_peso_kg NUMERIC(10,2) NOT NULL CHECK (capacidad_peso_kg > 0),
    volumen_maximo_m3 NUMERIC(10,2) NOT NULL CHECK (volumen_maximo_m3 > 0),
    zona_operacion    VARCHAR(20)  NOT NULL,   -- geohash precisión 3 (~156 km × 156 km)
    estado            estado_vehiculo NOT NULL DEFAULT 'DISPONIBLE',
    conductor_id      UUID,                    -- FK diferida hacia conductores (ver sección D)
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehiculos_tipo_estado   ON vehiculos (tipo, estado);
CREATE INDEX idx_vehiculos_zona_estado   ON vehiculos (zona_operacion, estado);

-- =============================================================================
-- C. TABLA conductores
-- =============================================================================

CREATE TABLE conductores (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre               VARCHAR(200)  NOT NULL,
    email                VARCHAR(255)  UNIQUE,
    modelo_contrato      modelo_contrato NOT NULL DEFAULT 'POR_PARADA',
    estado               estado_conductor NOT NULL DEFAULT 'ACTIVO',
    vehiculo_asignado_id UUID REFERENCES vehiculos (id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conductores_estado ON conductores (estado);

-- =============================================================================
-- D. FK CIRCULAR DIFERIDA — vehiculos ↔ conductores
-- DEFERRABLE INITIALLY DEFERRED permite insertar ambas tablas antes de
-- que PostgreSQL evalúe la restricción (se evalúa al COMMIT).
-- =============================================================================

ALTER TABLE vehiculos
    ADD CONSTRAINT fk_vehiculos_conductor
    FOREIGN KEY (conductor_id) REFERENCES conductores (id)
    DEFERRABLE INITIALLY DEFERRED;

-- =============================================================================
-- E. TABLA historial_asignaciones
-- Registro cronológico de qué conductor operó qué vehículo.
-- fecha_hora_fin NULL indica la asignación está actualmente activa.
-- =============================================================================

CREATE TABLE historial_asignaciones (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conductor_id      UUID        NOT NULL REFERENCES conductores (id),
    vehiculo_id       UUID        NOT NULL REFERENCES vehiculos (id),
    fecha_hora_inicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_hora_fin    TIMESTAMPTZ           -- NULL = asignación activa
);

CREATE INDEX idx_historial_conductor ON historial_asignaciones (conductor_id);

-- Partial index: permite buscar rápidamente la asignación activa de un conductor
CREATE INDEX idx_historial_activo ON historial_asignaciones (conductor_id)
    WHERE fecha_hora_fin IS NULL;

-- =============================================================================
-- F. TABLA rutas
-- =============================================================================

CREATE TABLE rutas (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    zona                    VARCHAR(20)   NOT NULL,   -- geohash precisión 5 (~4.9 km × 4.9 km)
    estado                  estado_ruta   NOT NULL DEFAULT 'CREADA',
    peso_acumulado_kg       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (peso_acumulado_kg >= 0),
    tipo_vehiculo_requerido tipo_vehiculo NOT NULL DEFAULT 'MOTO',
    vehiculo_id             UUID REFERENCES vehiculos (id),
    conductor_id            UUID REFERENCES conductores (id),
    fecha_creacion_ruta     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    fecha_limite_despacho   TIMESTAMPTZ   NOT NULL,
    fecha_hora_inicio       TIMESTAMPTZ,
    fecha_hora_cierre       TIMESTAMPTZ,
    tipo_cierre             tipo_cierre,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- CLAVE DE CONCURRENCIA: garantiza una sola ruta CREADA por zona geográfica.
-- El UNIQUE PARTIAL resuelve la concurrencia a nivel de BD sin locks de aplicación.
CREATE UNIQUE INDEX idx_rutas_zona_creada ON rutas (zona)
    WHERE estado = 'CREADA';

CREATE INDEX idx_rutas_estado       ON rutas (estado);

-- Usado por el scheduler de vencimiento de fecha límite
CREATE INDEX idx_rutas_fecha_limite ON rutas (fecha_limite_despacho)
    WHERE estado = 'CREADA';

-- Usado por el scheduler de cierre automático
CREATE INDEX idx_rutas_fecha_inicio ON rutas (fecha_hora_inicio)
    WHERE estado = 'EN_TRANSITO';

CREATE INDEX idx_rutas_conductor    ON rutas (conductor_id);

-- =============================================================================
-- G. TABLA paradas
-- Cada parada corresponde a un paquete del Módulo 1 (SGP).
-- paquete_id es una referencia externa — no hay FK hacia SGP.
-- =============================================================================

CREATE TABLE paradas (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    ruta_id              UUID          NOT NULL REFERENCES rutas (id),
    paquete_id           UUID          NOT NULL,           -- referencia externa al SGP (Módulo 1)
    orden                INTEGER       NOT NULL DEFAULT 0,
    direccion            VARCHAR(500)  NOT NULL,
    latitud              NUMERIC(10,8) NOT NULL,
    longitud             NUMERIC(11,8) NOT NULL,
    tipo_mercancia       VARCHAR(20),                      -- FRAGIL | PELIGROSO | ESTANDAR
    metodo_pago          VARCHAR(20),                      -- PREPAGO | CONTRA_ENTREGA
    fecha_limite_entrega TIMESTAMPTZ,
    estado               estado_parada NOT NULL DEFAULT 'PENDIENTE',
    motivo_novedad       motivo_novedad,
    fecha_hora_gestion   TIMESTAMPTZ,                      -- timestamp real registrado por el conductor (puede ser offline)
    firma_receptor_url   VARCHAR(1000),
    foto_evidencia_url   VARCHAR(1000),
    nombre_receptor      VARCHAR(200),
    origen               origen_parada NOT NULL DEFAULT 'CONDUCTOR',
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    UNIQUE (ruta_id, paquete_id)                           -- un paquete solo puede estar una vez por ruta
);

CREATE INDEX idx_paradas_ruta        ON paradas (ruta_id);
CREATE INDEX idx_paradas_ruta_estado ON paradas (ruta_id, estado);
CREATE INDEX idx_paradas_paquete     ON paradas (paquete_id);
