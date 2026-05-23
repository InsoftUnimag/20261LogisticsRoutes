-- =============================================================================
-- V10__reseed_demo.sql  —  Re-seed completo para demo del frontend
--
-- Limpia toda la data y resiembra con escenarios que cubren todos los flujos:
--   · Fleet Admin : vehículos y conductores en todos los estados posibles
--   · Dispatcher  : rutas en todos los estados del ciclo de vida
--   · Driver      : ruta EN_TRANSITO con paradas en todos los estados
--
-- Usuarios: mismos emails y hash BCrypt que V6. Password en claro: password123
--
-- UUID prefijos:
--   a0... → vehiculos     b0... → conductores    c0... → historial
--   d0... → rutas         e0... → paradas         f0... → paquetes (externos)
-- =============================================================================


-- =============================================================================
-- 0. LIMPIEZA (orden inverso a las FKs)
-- =============================================================================

DELETE FROM paradas;
DELETE FROM rutas;
DELETE FROM historial_asignaciones;
UPDATE usuarios    SET conductor_id        = NULL WHERE conductor_id        IS NOT NULL;
UPDATE conductores SET vehiculo_asignado_id = NULL WHERE vehiculo_asignado_id IS NOT NULL;
UPDATE vehiculos   SET conductor_id        = NULL WHERE conductor_id        IS NOT NULL;
DELETE FROM conductores;
DELETE FROM vehiculos;
DELETE FROM usuarios;


-- =============================================================================
-- 1. USUARIOS — mismos que V6, hash no cambia (password123)
-- =============================================================================

INSERT INTO usuarios (id, email, password_hash, rol, created_at) VALUES
    (gen_random_uuid(), 'admin@logisticasm.com',      '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'FLEET_ADMIN', NOW()),
    (gen_random_uuid(), 'dispatcher@logisticasm.com', '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'DISPATCHER',  NOW()),
    (gen_random_uuid(), 'driver@logisticasm.com',     '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'DRIVER',      NOW()),
    (gen_random_uuid(), 'system@logisticasm.com',     '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'SYSTEM',      NOW());


-- =============================================================================
-- 2. VEHÍCULOS (10) — conductor_id NULL; se actualiza en bloque 5
--
--   V01 MNT101 MOTO   EN_TRANSITO d3gpz — Juan Carlos (C01, driver user)
--   V02 VAN202 VAN    DISPONIBLE  d3gpz — María (C02)
--   V03 NHR303 NHR    DISPONIBLE  d3fy3 — Sergio (C03) — en ruta CONFIRMADA
--   V04 TRB404 TURBO  DISPONIBLE  d3f71 — Carlos (C07)
--   V05 MNT505 MOTO   DISPONIBLE  d3fy3 — sin conductor  (demo asignación)
--   V06 VAN606 VAN    DISPONIBLE  d3f71 — sin conductor  (demo asignación)
--   V07 MNT707 MOTO   EN_TRANSITO d3fy3 — Diana (C05)
--   V08 NHR808 NHR    INACTIVO    d3gpz — Laura (C06, INACTIVO)
--   V09 VAN909 VAN    INACTIVO    d3f71 — sin conductor
--   V10 MNT110 MOTO   DISPONIBLE  d3gpz — sin conductor  (demo asignación)
-- =============================================================================

INSERT INTO vehiculos (id, placa, tipo, modelo, capacidad_peso_kg, volumen_maximo_m3, zona_operacion, estado)
VALUES
    ('a0000000-0000-0000-0000-000000000001'::uuid,
     'MNT101', 'MOTO',  'Yamaha NMX 155',        45.00,   0.45, 'd3gpz', 'EN_TRANSITO'),

    ('a0000000-0000-0000-0000-000000000002'::uuid,
     'VAN202', 'VAN',   'Renault Kangoo',       1200.00,   4.50, 'd3gpz', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000003'::uuid,
     'NHR303', 'NHR',   'Chevrolet NHR',        3000.00,  15.00, 'd3fy3', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000004'::uuid,
     'TRB404', 'TURBO', 'Chevrolet NPR',        7000.00,  32.00, 'd3f71', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000005'::uuid,
     'MNT505', 'MOTO',  'Honda CB 160F',          35.00,   0.35, 'd3fy3', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000006'::uuid,
     'VAN606', 'VAN',   'Chevrolet N300 Max',   1000.00,   3.80, 'd3f71', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000007'::uuid,
     'MNT707', 'MOTO',  'Bajaj Boxer CT 100',     30.00,   0.30, 'd3fy3', 'EN_TRANSITO'),

    ('a0000000-0000-0000-0000-000000000008'::uuid,
     'NHR808', 'NHR',   'JAC 1040',             2800.00,  14.00, 'd3gpz', 'INACTIVO'),

    ('a0000000-0000-0000-0000-000000000009'::uuid,
     'VAN909', 'VAN',   'Renault Trafic',       1100.00,   5.00, 'd3f71', 'INACTIVO'),

    ('a0000000-0000-0000-0000-000000000010'::uuid,
     'MNT110', 'MOTO',  'Yamaha FZ 250',          50.00,   0.50, 'd3gpz', 'DISPONIBLE');


-- =============================================================================
-- 3. CONDUCTORES (8) — vehiculo_asignado_id NULL; se actualiza en bloque 5
--
--   C01 Juan Carlos  EN_RUTA   V01  — usuario driver@logisticasm.com
--   C02 María        ACTIVO    V02
--   C03 Sergio       ACTIVO    V03  — asignado a ruta CONFIRMADA
--   C04 Andrés       ACTIVO    ---  — sin vehículo (demo asignación)
--   C05 Diana        EN_RUTA   V07
--   C06 Laura        INACTIVO  V08
--   C07 Carlos       ACTIVO    V04
--   C08 Patricia     INACTIVO  ---  — sin vehículo
-- =============================================================================

INSERT INTO conductores (id, nombre, email, modelo_contrato, estado)
VALUES
    ('b0000000-0000-0000-0000-000000000001'::uuid,
     'Juan Carlos Martínez Ochoa',   'juan.martinez@logisticasm.com',   'RECORRIDO_COMPLETO', 'EN_RUTA'),

    ('b0000000-0000-0000-0000-000000000002'::uuid,
     'María Fernanda Gómez Ríos',    'maria.gomez@logisticasm.com',     'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000003'::uuid,
     'Sergio Alejandro Ruiz',        'sergio.ruiz@logisticasm.com',     'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000004'::uuid,
     'Andrés Felipe Restrepo Villa', 'andres.restrepo@logisticasm.com', 'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000005'::uuid,
     'Diana Patricia Hernández',     'diana.hernandez@logisticasm.com', 'POR_PARADA',         'EN_RUTA'),

    ('b0000000-0000-0000-0000-000000000006'::uuid,
     'Laura Catalina Páez Moreno',   'laura.paez@logisticasm.com',      'POR_PARADA',         'INACTIVO'),

    ('b0000000-0000-0000-0000-000000000007'::uuid,
     'Carlos Eduardo Mendoza',       'carlos.mendoza@logisticasm.com',  'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000008'::uuid,
     'Patricia López Bermúdez',      'patricia.lopez@logisticasm.com',  'POR_PARADA',         'INACTIVO');


-- =============================================================================
-- 4. HISTORIAL DE ASIGNACIONES
--   6 activas  (fecha_hora_fin IS NULL)  — estado actual del conductor/vehículo
--   3 históricas (fecha_hora_fin NOT NULL) — asignaciones cerradas en el pasado
-- =============================================================================

INSERT INTO historial_asignaciones (id, conductor_id, vehiculo_id, fecha_hora_inicio, fecha_hora_fin)
VALUES
    -- Asignaciones ACTIVAS
    ('c0000000-0000-0000-0000-000000000001'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,  -- Juan Carlos
     'a0000000-0000-0000-0000-000000000001'::uuid,  -- MNT101
     '2026-04-01 08:00:00-05', NULL),

    ('c0000000-0000-0000-0000-000000000002'::uuid,
     'b0000000-0000-0000-0000-000000000002'::uuid,  -- María
     'a0000000-0000-0000-0000-000000000002'::uuid,  -- VAN202
     '2026-03-15 08:00:00-05', NULL),

    ('c0000000-0000-0000-0000-000000000003'::uuid,
     'b0000000-0000-0000-0000-000000000003'::uuid,  -- Sergio
     'a0000000-0000-0000-0000-000000000003'::uuid,  -- NHR303
     '2026-02-01 08:00:00-05', NULL),

    ('c0000000-0000-0000-0000-000000000004'::uuid,
     'b0000000-0000-0000-0000-000000000005'::uuid,  -- Diana
     'a0000000-0000-0000-0000-000000000007'::uuid,  -- MNT707
     '2026-04-15 07:30:00-05', NULL),

    ('c0000000-0000-0000-0000-000000000005'::uuid,
     'b0000000-0000-0000-0000-000000000006'::uuid,  -- Laura
     'a0000000-0000-0000-0000-000000000008'::uuid,  -- NHR808
     '2026-01-10 08:00:00-05', NULL),

    ('c0000000-0000-0000-0000-000000000006'::uuid,
     'b0000000-0000-0000-0000-000000000007'::uuid,  -- Carlos
     'a0000000-0000-0000-0000-000000000004'::uuid,  -- TRB404
     '2026-03-01 08:00:00-05', NULL),

    -- Asignaciones HISTÓRICAS (cerradas)
    ('c0000000-0000-0000-0000-000000000007'::uuid,
     'b0000000-0000-0000-0000-000000000004'::uuid,  -- Andrés (ahora sin vehículo)
     'a0000000-0000-0000-0000-000000000005'::uuid,  -- MNT505
     '2026-01-01 08:00:00-05', '2026-02-28 18:00:00-05'),

    ('c0000000-0000-0000-0000-000000000008'::uuid,
     'b0000000-0000-0000-0000-000000000008'::uuid,  -- Patricia (INACTIVO, sin vehículo)
     'a0000000-0000-0000-0000-000000000009'::uuid,  -- VAN909
     '2025-11-01 08:00:00-05', '2026-01-09 17:00:00-05'),

    ('c0000000-0000-0000-0000-000000000009'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,  -- Juan Carlos (vehículo anterior)
     'a0000000-0000-0000-0000-000000000010'::uuid,  -- MNT110
     '2026-01-15 08:00:00-05', '2026-03-31 18:00:00-05');


-- =============================================================================
-- 5. ACTUALIZAR FK CIRCULAR (vehiculos ↔ conductores) para asignaciones ACTIVAS
-- =============================================================================

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000001'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000001'::uuid;  -- MNT101 → Juan Carlos

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000002'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000002'::uuid;  -- VAN202 → María

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000003'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000003'::uuid;  -- NHR303 → Sergio

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000007'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000004'::uuid;  -- TRB404 → Carlos

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000005'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000007'::uuid;  -- MNT707 → Diana

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000006'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000008'::uuid;  -- NHR808 → Laura

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000001'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000001'::uuid;  -- Juan Carlos → MNT101

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000002'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000002'::uuid;  -- María → VAN202

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000003'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000003'::uuid;  -- Sergio → NHR303

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000004'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000007'::uuid;  -- Carlos → TRB404

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000007'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000005'::uuid;  -- Diana → MNT707

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000008'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000006'::uuid;  -- Laura → NHR808


-- =============================================================================
-- 6. VINCULAR usuario DRIVER con el conductor Juan Carlos
-- =============================================================================

UPDATE usuarios
   SET conductor_id = 'b0000000-0000-0000-0000-000000000001'::uuid
 WHERE email = 'driver@logisticasm.com';


-- =============================================================================
-- 7. RUTAS (7) — cubren todo el ciclo de vida del despachador
--
--   R01 CREADA               d3gpz MOTO   — sin vehículo/conductor
--   R02 LISTA_PARA_DESPACHO  d3fy3 VAN    — sin vehículo/conductor
--   R03 CONFIRMADA           d3f71 NHR    — V03 NHR303 / C03 Sergio
--   R04 EN_TRANSITO          d3gpy MOTO   — V01 MNT101 / C01 Juan Carlos (driver)
--   R05 EN_TRANSITO          d3fy1 MOTO   — V07 MNT707 / C05 Diana
--   R06 CERRADA_MANUAL       d3gpx VAN    — V02 VAN202 / C02 María (histórica)
--   R07 CERRADA_AUTOMATICA   d3f72 NHR    — V04 TRB404 / C07 Carlos (histórica)
-- =============================================================================

INSERT INTO rutas (
    id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
    vehiculo_id, conductor_id,
    fecha_creacion_ruta, fecha_limite_despacho,
    fecha_hora_inicio, fecha_hora_cierre, tipo_cierre, motivo_despacho
)
VALUES
    -- R01: CREADA — esperando que el despachador la gestione
    ('d0000000-0000-0000-0000-000000000001'::uuid,
     'd3gpz', 'CREADA', 32.50, 'MOTO',
     NULL, NULL,
     '2026-05-09 06:00:00-05', '2026-05-09 18:00:00-05',
     NULL, NULL, NULL, NULL),

    -- R02: LISTA_PARA_DESPACHO — despachador la marcó lista manualmente
    ('d0000000-0000-0000-0000-000000000002'::uuid,
     'd3fy3', 'LISTA_PARA_DESPACHO', 450.00, 'VAN',
     NULL, NULL,
     '2026-05-09 05:30:00-05', '2026-05-10 12:00:00-05',
     NULL, NULL, NULL, 'PAQUETES_URGENTES'),

    -- R03: CONFIRMADA — despachador asignó vehículo y conductor, aún no inicia
    ('d0000000-0000-0000-0000-000000000003'::uuid,
     'd3f71', 'CONFIRMADA', 1200.00, 'NHR',
     'a0000000-0000-0000-0000-000000000003'::uuid,
     'b0000000-0000-0000-0000-000000000003'::uuid,
     '2026-05-09 05:00:00-05', '2026-05-09 16:00:00-05',
     NULL, NULL, NULL, 'VOLUMEN_ALTO'),

    -- R04: EN_TRANSITO — Juan Carlos en campo (ruta del driver user)
    ('d0000000-0000-0000-0000-000000000004'::uuid,
     'd3gpy', 'EN_TRANSITO', 28.00, 'MOTO',
     'a0000000-0000-0000-0000-000000000001'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,
     '2026-05-09 05:00:00-05', '2026-05-09 20:00:00-05',
     '2026-05-09 08:30:00-05', NULL, NULL, 'RUTA_PRIORITARIA'),

    -- R05: EN_TRANSITO — Diana en campo
    ('d0000000-0000-0000-0000-000000000005'::uuid,
     'd3fy1', 'EN_TRANSITO', 22.00, 'MOTO',
     'a0000000-0000-0000-0000-000000000007'::uuid,
     'b0000000-0000-0000-0000-000000000005'::uuid,
     '2026-05-09 05:15:00-05', '2026-05-09 19:00:00-05',
     '2026-05-09 09:00:00-05', NULL, NULL, 'CLIENTE_VIP'),

    -- R06: CERRADA_MANUAL — cerrada por el despachador (histórica)
    ('d0000000-0000-0000-0000-000000000006'::uuid,
     'd3gpx', 'CERRADA_MANUAL', 380.00, 'VAN',
     'a0000000-0000-0000-0000-000000000002'::uuid,
     'b0000000-0000-0000-0000-000000000002'::uuid,
     '2026-05-07 05:00:00-05', '2026-05-07 18:00:00-05',
     '2026-05-07 08:15:00-05', '2026-05-07 16:30:00-05', 'MANUAL', 'DESPACHO_PROGRAMADO'),

    -- R07: CERRADA_AUTOMATICA — cerrada por el sistema tras timeout (histórica)
    ('d0000000-0000-0000-0000-000000000007'::uuid,
     'd3f72', 'CERRADA_AUTOMATICA', 2100.00, 'NHR',
     'a0000000-0000-0000-0000-000000000004'::uuid,
     'b0000000-0000-0000-0000-000000000007'::uuid,
     '2026-05-06 05:00:00-05', '2026-05-06 18:00:00-05',
     '2026-05-06 08:00:00-05', '2026-05-06 23:00:00-05', 'AUTOMATICO', 'CARGA_COMPLETA');


-- =============================================================================
-- 8. PARADAS
-- Coordenadas de referencia:
--   Santa Marta  ≈ (11.2408, -74.2110)
--   Barranquilla ≈ (10.9640, -74.7964)
--   Cartagena    ≈ (10.3910, -75.5144)
-- =============================================================================

INSERT INTO paradas (
    id, ruta_id, paquete_id, orden, direccion,
    latitud, longitud, tipo_mercancia, metodo_pago, fecha_limite_entrega,
    estado, motivo_novedad, fecha_hora_gestion,
    firma_receptor_url, foto_evidencia_url, nombre_receptor, origen
)
VALUES

    -- ─── R04: EN_TRANSITO — Juan Carlos (6 paradas, estados variados) ───────

    ('e0000000-0000-0000-0000-000000000001'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000001'::uuid,
     1, 'Calle 22 #15-40, Centro, Santa Marta',
     11.24150, -74.20900, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-09 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000002'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000002'::uuid,
     2, 'Carrera 5 #18-20, El Prado, Santa Marta',
     11.23800, -74.21200, 'FRAGIL', 'PREPAGO', '2026-05-09 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000003'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000003'::uuid,
     3, 'Av. del Ferrocarril #30-12, Santa Marta',
     11.24500, -74.20600, 'ESTANDAR', 'PREPAGO', '2026-05-09 18:00:00-05',
     'EXITOSA', NULL, '2026-05-09 09:15:00-05',
     'https://storage.logisticasm.com/firmas/e03.png',
     'https://storage.logisticasm.com/fotos/e03.jpg',
     'Rosa Elena Vargas', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000004'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000004'::uuid,
     4, 'Calle 10 #8-55, Gaira, Santa Marta',
     11.24900, -74.20100, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-09 18:00:00-05',
     'EXITOSA', NULL, '2026-05-09 09:50:00-05',
     'https://storage.logisticasm.com/firmas/e04.png',
     'https://storage.logisticasm.com/fotos/e04.jpg',
     'Mauricio Suárez', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000005'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000005'::uuid,
     5, 'Calle 30 #23-10, Los Fundadores, Santa Marta',
     11.24200, -74.21500, 'ESTANDAR', 'PREPAGO', '2026-05-09 18:00:00-05',
     'FALLIDA', 'CLIENTE_AUSENTE', '2026-05-09 10:20:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e05.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000006'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid,
     'f0000000-0000-0000-0000-000000000006'::uuid,
     6, 'Carrera 19 #12-80, Mamatoco, Santa Marta',
     11.25100, -74.19800, 'PELIGROSO', 'PREPAGO', '2026-05-09 18:00:00-05',
     'NOVEDAD', 'DIRECCION_INCORRECTA', '2026-05-09 10:55:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e06.jpg',
     NULL, 'CONDUCTOR'),

    -- ─── R05: EN_TRANSITO — Diana (5 paradas) ────────────────────────────────

    ('e0000000-0000-0000-0000-000000000007'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid,
     'f0000000-0000-0000-0000-000000000007'::uuid,
     1, 'Calle 72 #46-21, El Prado, Barranquilla',
     10.97200, -74.80100, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-09 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000008'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid,
     'f0000000-0000-0000-0000-000000000008'::uuid,
     2, 'Carrera 53 #68-40, Bellavista, Barranquilla',
     10.96900, -74.79800, 'ESTANDAR', 'PREPAGO', '2026-05-09 19:00:00-05',
     'EXITOSA', NULL, '2026-05-09 09:30:00-05',
     'https://storage.logisticasm.com/firmas/e08.png',
     'https://storage.logisticasm.com/fotos/e08.jpg',
     'Claudia Inés Ramos', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000009'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid,
     'f0000000-0000-0000-0000-000000000009'::uuid,
     3, 'Calle 84 #52-10, Altos del Prado, Barranquilla',
     10.97600, -74.79500, 'FRAGIL', 'PREPAGO', '2026-05-09 19:00:00-05',
     'EXITOSA', NULL, '2026-05-09 10:05:00-05',
     'https://storage.logisticasm.com/firmas/e09.png',
     'https://storage.logisticasm.com/fotos/e09.jpg',
     'Felipe Andrade', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000010'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid,
     'f0000000-0000-0000-0000-000000000010'::uuid,
     4, 'Av. Murillo #34-55, La Victoria, Barranquilla',
     10.96500, -74.80500, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-09 19:00:00-05',
     'FALLIDA', 'RECHAZADO_POR_CLIENTE', '2026-05-09 10:40:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e10.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000011'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid,
     'f0000000-0000-0000-0000-000000000011'::uuid,
     5, 'Calle 45 #28-90, Barrio Abajo, Barranquilla',
     10.96100, -74.80900, 'ESTANDAR', 'PREPAGO', '2026-05-09 19:00:00-05',
     'NOVEDAD', 'ZONA_DIFICIL_ACCESO', '2026-05-09 11:10:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e11.jpg',
     NULL, 'CONDUCTOR'),

    -- ─── R01: CREADA — 3 paradas pendientes ──────────────────────────────────

    ('e0000000-0000-0000-0000-000000000012'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid,
     'f0000000-0000-0000-0000-000000000012'::uuid,
     1, 'Calle 18 #4-32, Centro Histórico, Santa Marta',
     11.24050, -74.21300, 'ESTANDAR', 'PREPAGO', '2026-05-09 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000013'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid,
     'f0000000-0000-0000-0000-000000000013'::uuid,
     2, 'Carrera 8 #22-15, El Rodadero, Santa Marta',
     11.24600, -74.20400, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-09 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000014'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid,
     'f0000000-0000-0000-0000-000000000014'::uuid,
     3, 'Av. Campo Serrano #11-80, Santa Marta',
     11.23600, -74.21800, 'ESTANDAR', 'PREPAGO', '2026-05-09 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ─── R02: LISTA_PARA_DESPACHO — 4 paradas pendientes ─────────────────────

    ('e0000000-0000-0000-0000-000000000015'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid,
     'f0000000-0000-0000-0000-000000000015'::uuid,
     1, 'Calle 70 #41-20, Ciudad Jardín, Barranquilla',
     10.97000, -74.80300, 'ESTANDAR', 'PREPAGO', '2026-05-10 12:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000016'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid,
     'f0000000-0000-0000-0000-000000000016'::uuid,
     2, 'Carrera 46 #76-50, Los Andes, Barranquilla',
     10.97400, -74.79600, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-10 12:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000017'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid,
     'f0000000-0000-0000-0000-000000000017'::uuid,
     3, 'Calle 85 #55-34, San José, Barranquilla',
     10.97800, -74.79200, 'FRAGIL', 'PREPAGO', '2026-05-10 12:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000018'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid,
     'f0000000-0000-0000-0000-000000000018'::uuid,
     4, 'Av. Olaya Herrera #63-15, Barranquilla',
     10.96700, -74.80700, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-10 12:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ─── R03: CONFIRMADA — 3 paradas pendientes ───────────────────────────────

    ('e0000000-0000-0000-0000-000000000019'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid,
     'f0000000-0000-0000-0000-000000000019'::uuid,
     1, 'Calle del Arsenal #8-141, Getsemaní, Cartagena',
     10.42100, -75.55000, 'ESTANDAR', 'PREPAGO', '2026-05-09 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000020'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid,
     'f0000000-0000-0000-0000-000000000020'::uuid,
     2, 'Av. del Lago #30-60, Manga, Cartagena',
     10.41700, -75.54600, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-09 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000021'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid,
     'f0000000-0000-0000-0000-000000000021'::uuid,
     3, 'Calle Larga #10-25, San Diego, Cartagena',
     10.42500, -75.54200, 'FRAGIL', 'PREPAGO', '2026-05-09 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ─── R06: CERRADA_MANUAL — 4 paradas finalizadas ─────────────────────────

    ('e0000000-0000-0000-0000-000000000022'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid,
     'f0000000-0000-0000-0000-000000000022'::uuid,
     1, 'Calle 12 #5-80, Pescaíto, Santa Marta',
     11.24300, -74.20700, 'ESTANDAR', 'PREPAGO', '2026-05-07 18:00:00-05',
     'EXITOSA', NULL, '2026-05-07 09:00:00-05',
     'https://storage.logisticasm.com/firmas/e22.png',
     'https://storage.logisticasm.com/fotos/e22.jpg',
     'Hernando Torres', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000023'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid,
     'f0000000-0000-0000-0000-000000000023'::uuid,
     2, 'Av. Libertador #28-55, Santa Marta',
     11.24700, -74.20300, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-07 18:00:00-05',
     'EXITOSA', NULL, '2026-05-07 09:45:00-05',
     'https://storage.logisticasm.com/firmas/e23.png',
     'https://storage.logisticasm.com/fotos/e23.jpg',
     'Gloria Orozco', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000024'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid,
     'f0000000-0000-0000-0000-000000000024'::uuid,
     3, 'Calle 20 #17-30, La Candelaria, Santa Marta',
     11.24000, -74.21600, 'ESTANDAR', 'PREPAGO', '2026-05-07 18:00:00-05',
     'FALLIDA', 'CLIENTE_AUSENTE', '2026-05-07 10:30:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e24.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000025'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid,
     'f0000000-0000-0000-0000-000000000025'::uuid,
     4, 'Carrera 10 #14-60, Los Almendros, Santa Marta',
     11.23900, -74.20800, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-07 18:00:00-05',
     'EXITOSA', NULL, '2026-05-07 11:10:00-05',
     'https://storage.logisticasm.com/firmas/e25.png',
     'https://storage.logisticasm.com/fotos/e25.jpg',
     'Ismael Cure', 'CONDUCTOR'),

    -- ─── R07: CERRADA_AUTOMATICA — 4 paradas finalizadas ─────────────────────

    ('e0000000-0000-0000-0000-000000000026'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid,
     'f0000000-0000-0000-0000-000000000026'::uuid,
     1, 'Calle 32 #22-10, Bocagrande, Cartagena',
     10.39500, -75.55500, 'ESTANDAR', 'PREPAGO', '2026-05-06 18:00:00-05',
     'EXITOSA', NULL, '2026-05-06 09:20:00-05',
     'https://storage.logisticasm.com/firmas/e26.png',
     'https://storage.logisticasm.com/fotos/e26.jpg',
     'Beatriz Salgado', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000027'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid,
     'f0000000-0000-0000-0000-000000000027'::uuid,
     2, 'Av. San Martín #5-42, Bocagrande, Cartagena',
     10.39200, -75.55800, 'FRAGIL', 'PREPAGO', '2026-05-06 18:00:00-05',
     'NOVEDAD', 'DAÑADO_EN_RUTA', '2026-05-06 10:05:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e27.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000028'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid,
     'f0000000-0000-0000-0000-000000000028'::uuid,
     3, 'Calle del Porvenir #3-15, El Centro, Cartagena',
     10.42300, -75.54800, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-06 18:00:00-05',
     'EXITOSA', NULL, '2026-05-06 10:55:00-05',
     'https://storage.logisticasm.com/firmas/e28.png',
     'https://storage.logisticasm.com/fotos/e28.jpg',
     'Jairo Bustamante', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000029'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid,
     'f0000000-0000-0000-0000-000000000029'::uuid,
     4, 'Calle Segunda de Badillo #36-67, Cartagena',
     10.42700, -75.54400, 'ESTANDAR', 'PREPAGO', '2026-05-06 18:00:00-05',
     'EXITOSA', NULL, '2026-05-06 11:40:00-05',
     'https://storage.logisticasm.com/firmas/e29.png',
     'https://storage.logisticasm.com/fotos/e29.jpg',
     'Natalia Díaz', 'CONDUCTOR');