-- =============================================================================
-- V2__seed.sql  —  Datos de prueba para desarrollo local
-- Contexto: flota operando en la costa Caribe colombiana
--           (Santa Marta d2g, Barranquilla d2f, Cartagena d2b)
--
-- MAPA DE UUIDs
--   Vehículos  : 550e8400-e29b-41d4-a716-4466554400{01..08}
--   Conductores: 550e8400-e29b-41d4-a716-4466554400{11..16}
--   Historial  : 550e8400-e29b-41d4-a716-4466554400{21..25}
--
-- ESTRATEGIA de FK circular (vehiculos.conductor_id ↔ conductores.vehiculo_asignado_id):
--   1. INSERT vehiculos  con conductor_id  = NULL
--   2. INSERT conductores con vehiculo_asignado_id = NULL
--   3. INSERT historial_asignaciones
--   4. UPDATE vehiculos  SET conductor_id  para asignaciones activas
--   5. UPDATE conductores SET vehiculo_asignado_id para asignaciones activas
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: VEHÍCULOS (8 registros)
--   3 MOTO | 2 VAN | 2 NHR | 1 TURBO
--   6 DISPONIBLE | 2 INACTIVO
--   Zonas: d2g = Santa Marta, d2f = Barranquilla, d2b = Cartagena
-- =============================================================================

INSERT INTO vehiculos (id, placa, tipo, modelo, capacidad_peso_kg, volumen_maximo_m3, zona_operacion, estado)
VALUES
    -- MOTOS
    ('550e8400-e29b-41d4-a716-446655440001'::uuid,
     'MNT478', 'MOTO', 'Yamaha NMX 155',
     45.00, 0.45, 'd2g', 'DISPONIBLE'),

    ('550e8400-e29b-41d4-a716-446655440002'::uuid,
     'BDR921', 'MOTO', 'Honda CB 160F',
     35.00, 0.35, 'd2f', 'DISPONIBLE'),

    ('550e8400-e29b-41d4-a716-446655440003'::uuid,
     'KLM305', 'MOTO', 'Bajaj Boxer CT 100',
     25.00, 0.25, 'd2b', 'INACTIVO'),

    -- VANS
    ('550e8400-e29b-41d4-a716-446655440004'::uuid,
     'JPA112', 'VAN', 'Renault Kangoo',
     1200.00, 4.50, 'd2g', 'DISPONIBLE'),

    ('550e8400-e29b-41d4-a716-446655440005'::uuid,
     'TRD584', 'VAN', 'Chevrolet N300 Max',
     1000.00, 3.80, 'd2f', 'DISPONIBLE'),

    -- NHR
    ('550e8400-e29b-41d4-a716-446655440006'::uuid,
     'RVK739', 'NHR', 'Chevrolet NHR',
     3000.00, 15.00, 'd2g', 'DISPONIBLE'),

    ('550e8400-e29b-41d4-a716-446655440007'::uuid,
     'PLM063', 'NHR', 'JAC 1040',
     2800.00, 14.00, 'd2b', 'INACTIVO'),

    -- TURBO
    ('550e8400-e29b-41d4-a716-446655440008'::uuid,
     'GHN452', 'TURBO', 'Chevrolet NPR',
     7000.00, 32.00, 'd2f', 'DISPONIBLE');


-- =============================================================================
-- BLOQUE 2: CONDUCTORES (6 registros)
--   5 ACTIVO | 1 INACTIVO
--   3 RECORRIDO_COMPLETO | 3 POR_PARADA
-- =============================================================================

INSERT INTO conductores (id, nombre, email, modelo_contrato, estado)
VALUES
    ('550e8400-e29b-41d4-a716-446655440011'::uuid,
     'Juan Carlos Martínez Ochoa',
     'juan.martinez@logisticasm.com',
     'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('550e8400-e29b-41d4-a716-446655440012'::uuid,
     'María Fernanda Gómez Ríos',
     'maria.gomez@logisticasm.com',
     'POR_PARADA', 'ACTIVO'),

    ('550e8400-e29b-41d4-a716-446655440013'::uuid,
     'Andrés Felipe Restrepo Villa',
     'andres.restrepo@logisticasm.com',
     'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('550e8400-e29b-41d4-a716-446655440014'::uuid,
     'Laura Catalina Páez Moreno',
     'laura.paez@logisticasm.com',
     'POR_PARADA', 'INACTIVO'),

    ('550e8400-e29b-41d4-a716-446655440015'::uuid,
     'Sergio Alejandro Ruiz Bermúdez',
     'sergio.ruiz@logisticasm.com',
     'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('550e8400-e29b-41d4-a716-446655440016'::uuid,
     'Diana Patricia Hernández Ariza',
     'diana.hernandez@logisticasm.com',
     'POR_PARADA', 'ACTIVO');


-- =============================================================================
-- BLOQUE 3: HISTORIAL DE ASIGNACIONES (5 registros)
--   3 activas  (fecha_hora_fin IS NULL)  — conductor operando su vehículo actual
--   2 históricas (fecha_hora_fin NOT NULL) — asignaciones ya cerradas
-- =============================================================================

INSERT INTO historial_asignaciones (id, conductor_id, vehiculo_id, fecha_hora_inicio, fecha_hora_fin)
VALUES
    -- Asignaciones ACTIVAS
    ('550e8400-e29b-41d4-a716-446655440021'::uuid,
     '550e8400-e29b-41d4-a716-446655440011'::uuid,   -- Juan Carlos Martínez
     '550e8400-e29b-41d4-a716-446655440001'::uuid,   -- MNT478 MOTO
     '2026-03-01 08:00:00-05', NULL),

    ('550e8400-e29b-41d4-a716-446655440022'::uuid,
     '550e8400-e29b-41d4-a716-446655440012'::uuid,   -- María Fernanda Gómez
     '550e8400-e29b-41d4-a716-446655440004'::uuid,   -- JPA112 VAN
     '2026-03-10 08:00:00-05', NULL),

    ('550e8400-e29b-41d4-a716-446655440023'::uuid,
     '550e8400-e29b-41d4-a716-446655440016'::uuid,   -- Diana Patricia Hernández
     '550e8400-e29b-41d4-a716-446655440002'::uuid,   -- BDR921 MOTO
     '2026-04-01 07:30:00-05', NULL),

    -- Asignaciones HISTÓRICAS (cerradas)
    ('550e8400-e29b-41d4-a716-446655440024'::uuid,
     '550e8400-e29b-41d4-a716-446655440013'::uuid,   -- Andrés Felipe Restrepo (ahora sin vehículo)
     '550e8400-e29b-41d4-a716-446655440005'::uuid,   -- TRD584 VAN
     '2026-01-15 08:00:00-05', '2026-02-28 18:00:00-05'),

    ('550e8400-e29b-41d4-a716-446655440025'::uuid,
     '550e8400-e29b-41d4-a716-446655440014'::uuid,   -- Laura Catalina Páez (ahora INACTIVO)
     '550e8400-e29b-41d4-a716-446655440001'::uuid,   -- MNT478 MOTO (antes de ser asignada a Juan Carlos)
     '2025-11-01 08:00:00-05', '2026-02-28 18:00:00-05');


-- =============================================================================
-- BLOQUE 4: ACTUALIZAR FK circular para asignaciones ACTIVAS
--   vehiculos.conductor_id  → quién opera ese vehículo hoy
--   conductores.vehiculo_asignado_id → qué vehículo tiene ese conductor hoy
-- =============================================================================

-- Vincular vehículos a su conductor activo
UPDATE vehiculos
   SET conductor_id = '550e8400-e29b-41d4-a716-446655440011'::uuid   -- Juan Carlos Martínez
 WHERE id = '550e8400-e29b-41d4-a716-446655440001'::uuid;            -- MNT478

UPDATE vehiculos
   SET conductor_id = '550e8400-e29b-41d4-a716-446655440012'::uuid   -- María Fernanda Gómez
 WHERE id = '550e8400-e29b-41d4-a716-446655440004'::uuid;            -- JPA112

UPDATE vehiculos
   SET conductor_id = '550e8400-e29b-41d4-a716-446655440016'::uuid   -- Diana Patricia Hernández
 WHERE id = '550e8400-e29b-41d4-a716-446655440002'::uuid;            -- BDR921

-- Vincular conductores a su vehículo asignado activo
UPDATE conductores
   SET vehiculo_asignado_id = '550e8400-e29b-41d4-a716-446655440001'::uuid   -- MNT478
 WHERE id = '550e8400-e29b-41d4-a716-446655440011'::uuid;                    -- Juan Carlos Martínez

UPDATE conductores
   SET vehiculo_asignado_id = '550e8400-e29b-41d4-a716-446655440004'::uuid   -- JPA112
 WHERE id = '550e8400-e29b-41d4-a716-446655440012'::uuid;                    -- María Fernanda Gómez

UPDATE conductores
   SET vehiculo_asignado_id = '550e8400-e29b-41d4-a716-446655440002'::uuid   -- BDR921
 WHERE id = '550e8400-e29b-41d4-a716-446655440016'::uuid;                    -- Diana Patricia Hernández
