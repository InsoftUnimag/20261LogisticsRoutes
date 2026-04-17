-- ─────────────────────────────────────────────────────────────────────────────
-- V4 — Seed de usuarios por rol para desarrollo y pruebas
-- Todos usan password: "password123" hasheado con BCrypt factor 10
--
-- conductor_id del usuario DRIVER apunta a Juan Carlos Martínez
-- (UUID fijo del V2__seed.sql de F2 — conductor activo con moto MNT478)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO usuarios (email, password_hash, rol, conductor_id) VALUES
    ('dispatcher@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DISPATCHER',  NULL),
    ('driver@test.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DRIVER',      '550e8400-e29b-41d4-a716-446655440011'),
    ('admin@test.com',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FLEET_ADMIN', NULL),
    ('system@test.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SYSTEM',      NULL);
