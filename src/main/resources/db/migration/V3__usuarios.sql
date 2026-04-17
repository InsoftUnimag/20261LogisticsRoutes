-- ─────────────────────────────────────────────────────────────────────────────
-- V3 — Tabla de usuarios del sistema para autenticación JWT
-- DEPENDE DE: V1__init.sql (tabla conductores) — F2 debe estar mergeada primero
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE usuarios (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(50)  NOT NULL,        -- DISPATCHER | DRIVER | FLEET_ADMIN | SYSTEM
    conductor_id  UUID         REFERENCES conductores(id),  -- null si no es conductor
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol   ON usuarios(rol);
