-- =============================================================================
-- demo-data-jefe.sql — Escenario completo
--
-- Este script NO es una migración Flyway. Se ejecuta MANUALMENTE en DBeaver
-- REQUISITOS PREVIOS:
--   - PostgreSQL corriendo (docker compose up -d)
--   - Migraciones V1..V9 aplicadas (arranca Spring Boot al menos una vez)
--   - Seed V2 cargado (vehículos y conductores base con UUIDs 550e8400-...)
--
-- CARACTERÍSTICAS:
--   - Idempotente: borra el escenario demo previo antes de insertar.
--   - UUIDs del demo usan prefijo 660e8400-... para no chocar con V2 (550e8400-...).
--   - Cubre los 7 estados de Ruta y los 6 estados de Parada.
--
-- ESCENARIO:
--   Hoy a las 09:00 AM, una flota en la costa Caribe colombiana operando
--   en Santa Marta (d3gpz), Barranquilla (d3fy3) y Cartagena (d3f71).
-- =============================================================================


-- =============================================================================
-- PASO 1: Limpieza del demo previo (orden inverso por FKs)
-- =============================================================================

DELETE FROM paradas WHERE ruta_id IN (
    SELECT id FROM rutas WHERE id::text LIKE '660e8400%'
);
DELETE FROM rutas WHERE id::text LIKE '660e8400%';

-- Restaurar estados base de vehículos/conductores tocados por el demo
UPDATE vehiculos SET estado = 'DISPONIBLE'
 WHERE id IN (
    '550e8400-e29b-41d4-a716-446655440001'::uuid,   -- MNT478 MOTO
    '550e8400-e29b-41d4-a716-446655440002'::uuid,   -- BDR921 MOTO
    '550e8400-e29b-41d4-a716-446655440004'::uuid,   -- JPA112 VAN
    '550e8400-e29b-41d4-a716-446655440006'::uuid    -- RVK739 NHR
 );

UPDATE conductores SET estado = 'ACTIVO'
 WHERE id IN (
    '550e8400-e29b-41d4-a716-446655440011'::uuid,   -- Juan Carlos
    '550e8400-e29b-41d4-a716-446655440012'::uuid,   -- María Fernanda
    '550e8400-e29b-41d4-a716-446655440015'::uuid,   -- Sergio
    '550e8400-e29b-41d4-a716-446655440016'::uuid    -- Diana Patricia
 );


-- =============================================================================
-- PASO 2: Emparejar conductor adicional con vehículo (FLEET_ADMIN)
-- Sergio Ruiz (sin vehículo en V2) <-> NHR RVK739 (sin conductor en V2)
-- =============================================================================

UPDATE vehiculos
   SET conductor_id = '550e8400-e29b-41d4-a716-446655440015'::uuid
 WHERE id = '550e8400-e29b-41d4-a716-446655440006'::uuid;

UPDATE conductores
   SET vehiculo_asignado_id = '550e8400-e29b-41d4-a716-446655440006'::uuid
 WHERE id = '550e8400-e29b-41d4-a716-446655440015'::uuid;


-- =============================================================================
-- PASO 3: RUTAS — una por cada estado del ciclo de vida
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 1 — CREADA (acumulando paquetes en Santa Marta, aún sin capacidad)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   fecha_creacion_ruta, fecha_limite_despacho, motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440001'::uuid,
    'd3gpz',
    'CREADA',
    18.50,
    'MOTO',
    NOW() - INTERVAL '90 minutes',
    NOW() + INTERVAL '5 days',
    NULL
);

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 2 — LISTA_PARA_DESPACHO (capacidad al 90%, esperando despachador)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   fecha_creacion_ruta, fecha_limite_despacho, motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440002'::uuid,
    'd3fy3',
    'LISTA_PARA_DESPACHO',
    275.00,
    'VAN',
    NOW() - INTERVAL '3 hours',
    NOW() + INTERVAL '4 days',
    'capacidad_maxima_alcanzada'
);

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 3 — LISTA_PARA_DESPACHO (vencimiento de plazo — motivo distinto)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   fecha_creacion_ruta, fecha_limite_despacho, motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440003'::uuid,
    'd3f71',
    'LISTA_PARA_DESPACHO',
    12.00,
    'MOTO',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '10 minutes',
    'vencimiento_plazo'
);

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 4 — CONFIRMADA (Diana + BDR921 lista para iniciar tránsito)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   vehiculo_id, conductor_id,
                   fecha_creacion_ruta, fecha_limite_despacho, motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440004'::uuid,
    'd3gpz',
    'CONFIRMADA',
    31.20,
    'MOTO',
    '550e8400-e29b-41d4-a716-446655440002'::uuid,   -- BDR921
    '550e8400-e29b-41d4-a716-446655440016'::uuid,   -- Diana Patricia
    NOW() - INTERVAL '6 hours',
    NOW() + INTERVAL '3 days',
    'capacidad_maxima_alcanzada'
);
-- Reflejar en vehiculo + conductor
UPDATE vehiculos   SET estado = 'EN_TRANSITO' WHERE id = '550e8400-e29b-41d4-a716-446655440002'::uuid;
UPDATE conductores SET estado = 'EN_RUTA'    WHERE id = '550e8400-e29b-41d4-a716-446655440016'::uuid;

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 5 — EN_TRANSITO (Juan Carlos con MNT478 actualmente entregando)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   vehiculo_id, conductor_id,
                   fecha_creacion_ruta, fecha_limite_despacho,
                   fecha_hora_inicio, motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440005'::uuid,
    'd3gpz',
    'EN_TRANSITO',
    42.00,
    'MOTO',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,   -- MNT478
    '550e8400-e29b-41d4-a716-446655440011'::uuid,   -- Juan Carlos
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '2 days',
    NOW() - INTERVAL '90 minutes',
    'capacidad_maxima_alcanzada'
);
UPDATE vehiculos   SET estado = 'EN_TRANSITO' WHERE id = '550e8400-e29b-41d4-a716-446655440001'::uuid;
UPDATE conductores SET estado = 'EN_RUTA'    WHERE id = '550e8400-e29b-41d4-a716-446655440011'::uuid;

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 6 — CERRADA_MANUAL (María cerró ayer con paradas mixtas)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   vehiculo_id, conductor_id,
                   fecha_creacion_ruta, fecha_limite_despacho,
                   fecha_hora_inicio, fecha_hora_cierre, tipo_cierre,
                   motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440006'::uuid,
    'd3fy3',
    'CERRADA_MANUAL',
    890.00,
    'VAN',
    '550e8400-e29b-41d4-a716-446655440004'::uuid,   -- JPA112
    '550e8400-e29b-41d4-a716-446655440012'::uuid,   -- María Fernanda
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '20 hours',
    NOW() - INTERVAL '40 hours',
    NOW() - INTERVAL '14 hours',
    'MANUAL',
    'capacidad_maxima_alcanzada'
);
-- María y JPA112 ya volvieron a estar disponibles → ya restaurados en PASO 1

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 7 — CERRADA_AUTOMATICA (excedió 2 días en tránsito, sistema la cerró)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   vehiculo_id, conductor_id,
                   fecha_creacion_ruta, fecha_limite_despacho,
                   fecha_hora_inicio, fecha_hora_cierre, tipo_cierre,
                   motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440007'::uuid,
    'd3f71',
    'CERRADA_AUTOMATICA',
    2400.00,
    'NHR',
    '550e8400-e29b-41d4-a716-446655440006'::uuid,   -- RVK739
    '550e8400-e29b-41d4-a716-446655440015'::uuid,   -- Sergio
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '1 day 12 hours',
    'AUTOMATICO',
    'vencimiento_plazo'
);

-- ─────────────────────────────────────────────────────────────────────────
-- RUTA 8 — CERRADA_FORZADA (despachador cerró por emergencia)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO rutas (id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
                   vehiculo_id, conductor_id,
                   fecha_creacion_ruta, fecha_limite_despacho,
                   fecha_hora_inicio, fecha_hora_cierre, tipo_cierre,
                   motivo_despacho)
VALUES (
    '660e8400-e29b-41d4-a716-446655440008'::uuid,
    'd3gpz',
    'CERRADA_FORZADA',
    20.00,
    'MOTO',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    '550e8400-e29b-41d4-a716-446655440011'::uuid,
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '5 days 12 hours',
    'FORZADO_DESPACHADOR',
    'capacidad_maxima_alcanzada'
);


-- =============================================================================
-- PASO 4: PARADAS — paquetes asociados a cada ruta
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 1 (CREADA) — todas PENDIENTE
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440101'::uuid,
     '660e8400-e29b-41d4-a716-446655440001'::uuid,
     '660e8400-e29b-41d4-a716-446655440901'::uuid,
     0, 'Cra 4 #14-25, Santa Marta',
     11.24080000, -74.21100000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '3 days', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440102'::uuid,
     '660e8400-e29b-41d4-a716-446655440001'::uuid,
     '660e8400-e29b-41d4-a716-446655440902'::uuid,
     0, 'Calle 22 #5-18, Santa Marta',
     11.25210000, -74.20640000,
     'FRAGIL', 'CONTRA_ENTREGA',
     NOW() + INTERVAL '2 days', 'PENDIENTE', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 2 (LISTA_PARA_DESPACHO, capacidad 90%)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440201'::uuid,
     '660e8400-e29b-41d4-a716-446655440002'::uuid,
     '660e8400-e29b-41d4-a716-446655440911'::uuid,
     0, 'Cra 53 #80-100, Barranquilla',
     10.99500000, -74.81100000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '2 days', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440202'::uuid,
     '660e8400-e29b-41d4-a716-446655440002'::uuid,
     '660e8400-e29b-41d4-a716-446655440912'::uuid,
     0, 'Calle 84 #50-30, Barranquilla',
     10.99800000, -74.80700000,
     'ESTANDAR', 'CONTRA_ENTREGA',
     NOW() + INTERVAL '2 days', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440203'::uuid,
     '660e8400-e29b-41d4-a716-446655440002'::uuid,
     '660e8400-e29b-41d4-a716-446655440913'::uuid,
     0, 'Vía 40 #76-200, Barranquilla',
     11.00100000, -74.81700000,
     'PELIGROSO', 'PREPAGO',
     NOW() + INTERVAL '3 days', 'PENDIENTE', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 3 (LISTA por vencimiento, una sola parada)
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440301'::uuid,
     '660e8400-e29b-41d4-a716-446655440003'::uuid,
     '660e8400-e29b-41d4-a716-446655440921'::uuid,
     0, 'Centro Histórico, Cartagena',
     10.42500000, -75.55000000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '1 day', 'PENDIENTE', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 4 (CONFIRMADA) — orden ya optimizado
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440401'::uuid,
     '660e8400-e29b-41d4-a716-446655440004'::uuid,
     '660e8400-e29b-41d4-a716-446655440931'::uuid,
     1, 'Cra 1 #10-50, Santa Marta',
     11.24100000, -74.21000000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '2 days', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440402'::uuid,
     '660e8400-e29b-41d4-a716-446655440004'::uuid,
     '660e8400-e29b-41d4-a716-446655440932'::uuid,
     2, 'Cra 4 #18-30, Santa Marta',
     11.24400000, -74.20800000,
     'FRAGIL', 'CONTRA_ENTREGA',
     NOW() + INTERVAL '2 days', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440403'::uuid,
     '660e8400-e29b-41d4-a716-446655440004'::uuid,
     '660e8400-e29b-41d4-a716-446655440933'::uuid,
     3, 'Calle 15 #2-100, Santa Marta',
     11.24800000, -74.20500000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '3 days', 'PENDIENTE', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 5 (EN_TRANSITO) — mezcla: 1 entregada, 2 pendientes
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega,
                     estado, fecha_hora_gestion,
                     foto_evidencia_url, firma_receptor_url, nombre_receptor, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440501'::uuid,
     '660e8400-e29b-41d4-a716-446655440005'::uuid,
     '660e8400-e29b-41d4-a716-446655440941'::uuid,
     1, 'Cra 2 #16-40, Santa Marta',
     11.24200000, -74.21200000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '1 day',
     'EXITOSA', NOW() - INTERVAL '60 minutes',
     'https://storage.local/demo/foto-501.jpg',
     'https://storage.local/demo/firma-501.png',
     'Ana María Rodríguez', 'CONDUCTOR');

INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440502'::uuid,
     '660e8400-e29b-41d4-a716-446655440005'::uuid,
     '660e8400-e29b-41d4-a716-446655440942'::uuid,
     2, 'Calle 20 #3-50, Santa Marta',
     11.24500000, -74.21000000,
     'FRAGIL', 'CONTRA_ENTREGA',
     NOW() + INTERVAL '1 day', 'PENDIENTE', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440503'::uuid,
     '660e8400-e29b-41d4-a716-446655440005'::uuid,
     '660e8400-e29b-41d4-a716-446655440943'::uuid,
     3, 'Cra 5 #22-15, Santa Marta',
     11.25000000, -74.20800000,
     'ESTANDAR', 'PREPAGO',
     NOW() + INTERVAL '1 day', 'PENDIENTE', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 6 (CERRADA_MANUAL) — diversidad de outcomes
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega,
                     estado, fecha_hora_gestion,
                     foto_evidencia_url, firma_receptor_url, nombre_receptor, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440601'::uuid,
     '660e8400-e29b-41d4-a716-446655440006'::uuid,
     '660e8400-e29b-41d4-a716-446655440951'::uuid,
     1, 'Cra 50 #76-120, Barranquilla',
     10.99000000, -74.80500000,
     'ESTANDAR', 'PREPAGO',
     NOW() - INTERVAL '1 day',
     'EXITOSA', NOW() - INTERVAL '38 hours',
     'https://storage.local/demo/foto-601.jpg',
     'https://storage.local/demo/firma-601.png',
     'Carlos Mendoza', 'CONDUCTOR');

INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega,
                     estado, motivo_novedad, fecha_hora_gestion, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440602'::uuid,
     '660e8400-e29b-41d4-a716-446655440006'::uuid,
     '660e8400-e29b-41d4-a716-446655440952'::uuid,
     2, 'Calle 72 #45-30, Barranquilla',
     10.99500000, -74.80100000,
     'FRAGIL', 'CONTRA_ENTREGA',
     NOW() - INTERVAL '1 day',
     'FALLIDA', 'CLIENTE_AUSENTE', NOW() - INTERVAL '34 hours', 'CONDUCTOR'),

    ('660e8400-e29b-41d4-a716-446655440603'::uuid,
     '660e8400-e29b-41d4-a716-446655440006'::uuid,
     '660e8400-e29b-41d4-a716-446655440953'::uuid,
     3, 'Cra 38 #80-15, Barranquilla',
     11.00200000, -74.80800000,
     'PELIGROSO', 'PREPAGO',
     NOW() - INTERVAL '1 day',
     'NOVEDAD', 'ZONA_DIFICIL_ACCESO', NOW() - INTERVAL '30 hours', 'CONDUCTOR'),

    ('660e8400-e29b-41d4-a716-446655440604'::uuid,
     '660e8400-e29b-41d4-a716-446655440006'::uuid,
     '660e8400-e29b-41d4-a716-446655440954'::uuid,
     4, 'Calle 84 #51-22, Barranquilla',
     11.00500000, -74.81000000,
     'ESTANDAR', 'PREPAGO',
     NOW() - INTERVAL '1 day',
     'SIN_GESTION_CONDUCTOR', NULL, NOW() - INTERVAL '14 hours', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Paradas para RUTA 7 (CERRADA_AUTOMATICA) — todas SIN_GESTION
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega,
                     estado, fecha_hora_gestion, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440701'::uuid,
     '660e8400-e29b-41d4-a716-446655440007'::uuid,
     '660e8400-e29b-41d4-a716-446655440961'::uuid,
     1, 'Bocagrande Calle 5 #2-50, Cartagena',
     10.39800000, -75.55500000,
     'ESTANDAR', 'PREPAGO',
     NOW() - INTERVAL '2 days',
     'SIN_GESTION_CONDUCTOR', NOW() - INTERVAL '1 day 12 hours', 'SISTEMA'),

    ('660e8400-e29b-41d4-a716-446655440702'::uuid,
     '660e8400-e29b-41d4-a716-446655440007'::uuid,
     '660e8400-e29b-41d4-a716-446655440962'::uuid,
     2, 'Manga Cra 24 #25-100, Cartagena',
     10.40500000, -75.54200000,
     'FRAGIL', 'CONTRA_ENTREGA',
     NOW() - INTERVAL '2 days',
     'SIN_GESTION_CONDUCTOR', NOW() - INTERVAL '1 day 12 hours', 'SISTEMA');

-- ─────────────────────────────────────────────────────────────────────────
-- Parada para RUTA 8 (CERRADA_FORZADA) — una exitosa + una excluida
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega,
                     estado, fecha_hora_gestion,
                     foto_evidencia_url, nombre_receptor, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440801'::uuid,
     '660e8400-e29b-41d4-a716-446655440008'::uuid,
     '660e8400-e29b-41d4-a716-446655440971'::uuid,
     1, 'Calle 17 #4-60, Santa Marta',
     11.24300000, -74.20900000,
     'ESTANDAR', 'PREPAGO',
     NOW() - INTERVAL '5 days',
     'EXITOSA', NOW() - INTERVAL '5 days 18 hours',
     'https://storage.local/demo/foto-801.jpg',
     'Pedro Castaño', 'CONDUCTOR');

INSERT INTO paradas (id, ruta_id, paquete_id, orden, direccion, latitud, longitud,
                     tipo_mercancia, metodo_pago, fecha_limite_entrega, estado, origen)
VALUES
    ('660e8400-e29b-41d4-a716-446655440802'::uuid,
     '660e8400-e29b-41d4-a716-446655440008'::uuid,
     '660e8400-e29b-41d4-a716-446655440972'::uuid,
     0, 'Cra 5 #16-30, Santa Marta',
     11.24500000, -74.21100000,
     'PELIGROSO', 'CONTRA_ENTREGA',
     NOW() - INTERVAL '5 days', 'EXCLUIDA_DESPACHO', 'SISTEMA');


-- =============================================================================
-- PASO 5: Resumen — query final para verificar que todo quedó bien
-- =============================================================================

SELECT
    r.id,
    r.zona,
    r.estado,
    r.peso_acumulado_kg AS peso_kg,
    r.tipo_vehiculo_requerido AS tipo_req,
    r.motivo_despacho,
    COUNT(p.id) AS total_paradas,
    COUNT(p.id) FILTER (WHERE p.estado = 'EXITOSA')              AS exitosas,
    COUNT(p.id) FILTER (WHERE p.estado = 'FALLIDA')              AS fallidas,
    COUNT(p.id) FILTER (WHERE p.estado = 'NOVEDAD')              AS novedades,
    COUNT(p.id) FILTER (WHERE p.estado = 'PENDIENTE')            AS pendientes,
    COUNT(p.id) FILTER (WHERE p.estado = 'SIN_GESTION_CONDUCTOR') AS sin_gestion,
    COUNT(p.id) FILTER (WHERE p.estado = 'EXCLUIDA_DESPACHO')    AS excluidas
FROM rutas r
LEFT JOIN paradas p ON p.ruta_id = r.id
WHERE r.id::text LIKE '660e8400%'
GROUP BY r.id, r.zona, r.estado, r.peso_acumulado_kg, r.tipo_vehiculo_requerido, r.motivo_despacho
ORDER BY r.estado, r.fecha_creacion_ruta;
