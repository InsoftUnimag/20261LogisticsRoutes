-- =============================================================================
-- V12__reseed_demo_v2.sql  —  Re-seed ampliado para demo del despachador
--
-- Limpia toda la data EXCEPTO usuarios y resiembra con un escenario rico:
--   · 18 vehiculos  (DISPONIBLE, EN_TRANSITO, INACTIVO — varios sin conductor)
--   · 14 conductores (ACTIVO, EN_RUTA, INACTIVO — varios sin vehiculo)
--   ·  4 rutas CREADA              (sin v/c, disponibles para gestionar)
--   ·  4 rutas LISTA_PARA_DESPACHO (sin v/c, listas para asignar v/c)
--   ·  2 rutas CONFIRMADA          (v/c asignados, sin iniciar)
--   ·  2 rutas EN_TRANSITO         (conductores en campo, paradas mixtas)
--   ·  2 rutas CERRADA             (historial)
--   · 62 paradas en total
--
-- UUID prefijos:
--   a0... vehiculos   b0... conductores   c0... historial
--   d0... rutas       e0... paradas
--
-- Fecha de referencia demo : 2026-05-15
-- Password usuarios (sin cambio): password123
-- =============================================================================


-- =============================================================================
-- 0. LIMPIEZA — todo excepto usuarios
-- =============================================================================

DELETE FROM paradas;
DELETE FROM rutas;
DELETE FROM historial_asignaciones;
UPDATE usuarios    SET conductor_id        = NULL WHERE conductor_id        IS NOT NULL;
UPDATE conductores SET vehiculo_asignado_id = NULL WHERE vehiculo_asignado_id IS NOT NULL;
UPDATE vehiculos   SET conductor_id        = NULL WHERE conductor_id        IS NOT NULL;
DELETE FROM conductores;
DELETE FROM vehiculos;


-- =============================================================================
-- 1. VEHICULOS (18)
--
--   V01 PJK-001 MOTO   EN_TRANSITO -> C01 Juan Carlos (driver user, ruta R11)
--   V02 PJK-002 MOTO   EN_TRANSITO -> C02 Diana (ruta R12)
--   V03 VNZ-003 VAN    DISPONIBLE  -> C03 Maria  (par disponible)
--   V04 VNZ-004 VAN    DISPONIBLE  -> C04 Sergio (par disponible)
--   V05 NHR-005 NHR    DISPONIBLE  -> C05 Carlos (par disponible)
--   V06 TRB-006 TURBO  DISPONIBLE  -> C06 Andres (par disponible)
--   V07 PJK-007 MOTO   DISPONIBLE  -> sin conductor (libre para asignar)
--   V08 PJK-008 MOTO   DISPONIBLE  -> C08 Valeria (asignado a R09 CONFIRMADA)
--   V09 VNZ-009 VAN    DISPONIBLE  -> sin conductor (libre, historial en R13)
--   V10 VNZ-010 VAN    DISPONIBLE  -> sin conductor (libre, historial en R14)
--   V11 NHR-011 NHR    DISPONIBLE  -> C09 Nicolas (asignado a R10 CONFIRMADA)
--   V12 TRB-012 TURBO  DISPONIBLE  -> sin conductor (libre para asignar)
--   V13 PJK-013 MOTO   DISPONIBLE  -> sin conductor (libre para asignar)
--   V14 VNZ-014 VAN    DISPONIBLE  -> sin conductor (libre para asignar)
--   V15 NHR-015 NHR    INACTIVO    -> C10 Laura (INACTIVO)
--   V16 PJK-016 MOTO   INACTIVO    -> sin conductor
--   V17 VNZ-017 VAN    INACTIVO    -> C11 Patricia (INACTIVO)
--   V18 TRB-018 TURBO  INACTIVO    -> sin conductor
-- =============================================================================

INSERT INTO vehiculos (id, placa, tipo, modelo, capacidad_peso_kg, volumen_maximo_m3, zona_operacion, estado)
VALUES
    ('a0000000-0000-0000-0000-000000000001'::uuid,
     'PJK-001', 'MOTO',  'Yamaha NMX 155',          45.00,  0.45, 'd3gpz', 'EN_TRANSITO'),

    ('a0000000-0000-0000-0000-000000000002'::uuid,
     'PJK-002', 'MOTO',  'Bajaj Boxer CT 100',       35.00,  0.35, 'd3fy3', 'EN_TRANSITO'),

    ('a0000000-0000-0000-0000-000000000003'::uuid,
     'VNZ-003', 'VAN',   'Renault Kangoo',         1200.00,  4.50, 'd3fy2', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000004'::uuid,
     'VNZ-004', 'VAN',   'Chevrolet N300 Max',     1000.00,  3.80, 'd3fy1', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000005'::uuid,
     'NHR-005', 'NHR',   'Chevrolet NHR',          3000.00, 15.00, 'd3f71', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000006'::uuid,
     'TRB-006', 'TURBO', 'Chevrolet NPR',          7000.00, 32.00, 'd3gpx', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000007'::uuid,
     'PJK-007', 'MOTO',  'Honda CB 160F',            40.00,  0.40, 'd3gpy', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000008'::uuid,
     'PJK-008', 'MOTO',  'Yamaha FZ 250',            50.00,  0.50, 'd3fy4', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000009'::uuid,
     'VNZ-009', 'VAN',   'Renault Trafic',         1100.00,  5.00, 'd3gpy', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000010'::uuid,
     'VNZ-010', 'VAN',   'Ford Transit',           1300.00,  6.00, 'd3fy1', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000011'::uuid,
     'NHR-011', 'NHR',   'JAC 1040',               2800.00, 14.00, 'd3f72', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000012'::uuid,
     'TRB-012', 'TURBO', 'Hino 300 Series',        6500.00, 30.00, 'd3gpx', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000013'::uuid,
     'PJK-013', 'MOTO',  'Suzuki GN 125H',           30.00,  0.30, 'd3gpu', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000014'::uuid,
     'VNZ-014', 'VAN',   'Hyundai H100',           1050.00,  4.20, 'd3fy2', 'DISPONIBLE'),

    ('a0000000-0000-0000-0000-000000000015'::uuid,
     'NHR-015', 'NHR',   'JMC Carrying',           2500.00, 12.00, 'd3f73', 'INACTIVO'),

    ('a0000000-0000-0000-0000-000000000016'::uuid,
     'PJK-016', 'MOTO',  'AKT 200',                  28.00,  0.28, 'd3gpz', 'INACTIVO'),

    ('a0000000-0000-0000-0000-000000000017'::uuid,
     'VNZ-017', 'VAN',   'Volkswagen Transporter', 1150.00,  5.50, 'd3fy3', 'INACTIVO'),

    ('a0000000-0000-0000-0000-000000000018'::uuid,
     'TRB-018', 'TURBO', 'Kenworth T270',          7500.00, 35.00, 'd3f74', 'INACTIVO');


-- =============================================================================
-- 2. CONDUCTORES (14)
--
--   C01 Juan Carlos  EN_RUTA  -> V01  (vinculado a driver@logisticasm.com)
--   C02 Diana        EN_RUTA  -> V02
--   C03 Maria        ACTIVO   -> V03  (par disponible)
--   C04 Sergio       ACTIVO   -> V04  (par disponible)
--   C05 Carlos       ACTIVO   -> V05  (par disponible)
--   C06 Andres       ACTIVO   -> V06  (par disponible)
--   C07 Felipe       ACTIVO   -> sin vehiculo (libre para asignar)
--   C08 Valeria      ACTIVO   -> V08  (asignado a R09 CONFIRMADA)
--   C09 Nicolas      ACTIVO   -> V11  (asignado a R10 CONFIRMADA)
--   C10 Laura        INACTIVO -> V15
--   C11 Patricia     INACTIVO -> V17
--   C12 Roberto      ACTIVO   -> sin vehiculo (libre, historial en R13)
--   C13 Camila       ACTIVO   -> sin vehiculo (libre, historial en R14)
--   C14 Eduardo      ACTIVO   -> sin vehiculo (libre para asignar)
-- =============================================================================

INSERT INTO conductores (id, nombre, email, modelo_contrato, estado)
VALUES
    ('b0000000-0000-0000-0000-000000000001'::uuid,
     'Juan Carlos Martinez Ochoa',   'juan.martinez@logisticasm.com',    'RECORRIDO_COMPLETO', 'EN_RUTA'),

    ('b0000000-0000-0000-0000-000000000002'::uuid,
     'Diana Patricia Hernandez',     'diana.hernandez@logisticasm.com',  'RECORRIDO_COMPLETO', 'EN_RUTA'),

    ('b0000000-0000-0000-0000-000000000003'::uuid,
     'Maria Fernanda Gomez Rios',    'maria.gomez@logisticasm.com',      'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000004'::uuid,
     'Sergio Alejandro Ruiz Mora',   'sergio.ruiz@logisticasm.com',      'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000005'::uuid,
     'Carlos Eduardo Mendoza Pinto', 'carlos.mendoza@logisticasm.com',   'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000006'::uuid,
     'Andres Felipe Restrepo Villa', 'andres.restrepo@logisticasm.com',  'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000007'::uuid,
     'Felipe Augusto Torres Cano',   'felipe.torres@logisticasm.com',    'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000008'::uuid,
     'Valeria Sofia Pedraza Nino',   'valeria.pedraza@logisticasm.com',  'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000009'::uuid,
     'Nicolas Esteban Vargas Leon',  'nicolas.vargas@logisticasm.com',   'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000010'::uuid,
     'Laura Catalina Paez Moreno',   'laura.paez@logisticasm.com',       'POR_PARADA',         'INACTIVO'),

    ('b0000000-0000-0000-0000-000000000011'::uuid,
     'Patricia Lopez Bermudez',      'patricia.lopez@logisticasm.com',   'POR_PARADA',         'INACTIVO'),

    ('b0000000-0000-0000-0000-000000000012'::uuid,
     'Roberto Enrique Suarez Daza',  'roberto.suarez@logisticasm.com',   'POR_PARADA',         'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000013'::uuid,
     'Camila Alejandra Ortiz Ramos', 'camila.ortiz@logisticasm.com',     'RECORRIDO_COMPLETO', 'ACTIVO'),

    ('b0000000-0000-0000-0000-000000000014'::uuid,
     'Eduardo Javier Cardenas Mora', 'eduardo.cardenas@logisticasm.com', 'POR_PARADA',         'ACTIVO');


-- =============================================================================
-- 3. HISTORIAL DE ASIGNACIONES (16 registros)
--   10 activas  (fecha_hora_fin IS NULL)
--    6 historicas (fecha_hora_fin NOT NULL)
-- =============================================================================

INSERT INTO historial_asignaciones (id, conductor_id, vehiculo_id, fecha_hora_inicio, fecha_hora_fin)
VALUES
    -- ACTIVAS
    ('c0000000-0000-0000-0000-000000000001'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,
     'a0000000-0000-0000-0000-000000000001'::uuid,
     '2026-04-01 08:00:00-05', NULL),   -- Juan Carlos -> PJK-001

    ('c0000000-0000-0000-0000-000000000002'::uuid,
     'b0000000-0000-0000-0000-000000000002'::uuid,
     'a0000000-0000-0000-0000-000000000002'::uuid,
     '2026-03-15 08:00:00-05', NULL),   -- Diana -> PJK-002

    ('c0000000-0000-0000-0000-000000000003'::uuid,
     'b0000000-0000-0000-0000-000000000003'::uuid,
     'a0000000-0000-0000-0000-000000000003'::uuid,
     '2026-02-01 08:00:00-05', NULL),   -- Maria -> VNZ-003

    ('c0000000-0000-0000-0000-000000000004'::uuid,
     'b0000000-0000-0000-0000-000000000004'::uuid,
     'a0000000-0000-0000-0000-000000000004'::uuid,
     '2026-03-01 08:00:00-05', NULL),   -- Sergio -> VNZ-004

    ('c0000000-0000-0000-0000-000000000005'::uuid,
     'b0000000-0000-0000-0000-000000000005'::uuid,
     'a0000000-0000-0000-0000-000000000005'::uuid,
     '2026-02-15 08:00:00-05', NULL),   -- Carlos -> NHR-005

    ('c0000000-0000-0000-0000-000000000006'::uuid,
     'b0000000-0000-0000-0000-000000000006'::uuid,
     'a0000000-0000-0000-0000-000000000006'::uuid,
     '2026-03-20 08:00:00-05', NULL),   -- Andres -> TRB-006

    ('c0000000-0000-0000-0000-000000000007'::uuid,
     'b0000000-0000-0000-0000-000000000008'::uuid,
     'a0000000-0000-0000-0000-000000000008'::uuid,
     '2026-04-10 08:00:00-05', NULL),   -- Valeria -> PJK-008

    ('c0000000-0000-0000-0000-000000000008'::uuid,
     'b0000000-0000-0000-0000-000000000009'::uuid,
     'a0000000-0000-0000-0000-000000000011'::uuid,
     '2026-04-05 08:00:00-05', NULL),   -- Nicolas -> NHR-011

    ('c0000000-0000-0000-0000-000000000009'::uuid,
     'b0000000-0000-0000-0000-000000000010'::uuid,
     'a0000000-0000-0000-0000-000000000015'::uuid,
     '2026-01-10 08:00:00-05', NULL),   -- Laura -> NHR-015 (INACTIVO)

    ('c0000000-0000-0000-0000-000000000010'::uuid,
     'b0000000-0000-0000-0000-000000000011'::uuid,
     'a0000000-0000-0000-0000-000000000017'::uuid,
     '2025-12-01 08:00:00-05', NULL),   -- Patricia -> VNZ-017 (INACTIVO)

    -- HISTORICAS — conductores y vehiculos ahora libres
    ('c0000000-0000-0000-0000-000000000011'::uuid,
     'b0000000-0000-0000-0000-000000000007'::uuid,
     'a0000000-0000-0000-0000-000000000007'::uuid,
     '2026-01-01 08:00:00-05', '2026-04-01 18:00:00-05'),  -- Felipe tuvo PJK-007

    ('c0000000-0000-0000-0000-000000000012'::uuid,
     'b0000000-0000-0000-0000-000000000012'::uuid,
     'a0000000-0000-0000-0000-000000000009'::uuid,
     '2026-01-15 08:00:00-05', '2026-04-30 18:00:00-05'),  -- Roberto tuvo VNZ-009

    ('c0000000-0000-0000-0000-000000000013'::uuid,
     'b0000000-0000-0000-0000-000000000013'::uuid,
     'a0000000-0000-0000-0000-000000000010'::uuid,
     '2026-01-20 08:00:00-05', '2026-04-25 18:00:00-05'),  -- Camila tuvo VNZ-010

    ('c0000000-0000-0000-0000-000000000014'::uuid,
     'b0000000-0000-0000-0000-000000000014'::uuid,
     'a0000000-0000-0000-0000-000000000012'::uuid,
     '2025-11-01 08:00:00-05', '2026-03-31 18:00:00-05'),  -- Eduardo tuvo TRB-012

    ('c0000000-0000-0000-0000-000000000015'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,
     'a0000000-0000-0000-0000-000000000013'::uuid,
     '2025-10-01 08:00:00-05', '2025-12-31 18:00:00-05'),  -- Juan Carlos antes tenia PJK-013

    ('c0000000-0000-0000-0000-000000000016'::uuid,
     'b0000000-0000-0000-0000-000000000002'::uuid,
     'a0000000-0000-0000-0000-000000000014'::uuid,
     '2025-11-01 08:00:00-05', '2026-03-14 18:00:00-05');  -- Diana antes tenia VNZ-014


-- =============================================================================
-- 4. ACTUALIZAR FK CIRCULAR (vehiculos <-> conductores) — asignaciones activas
-- =============================================================================

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000001'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000001'::uuid;  -- PJK-001 -> Juan Carlos

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000002'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000002'::uuid;  -- PJK-002 -> Diana

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000003'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000003'::uuid;  -- VNZ-003 -> Maria

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000004'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000004'::uuid;  -- VNZ-004 -> Sergio

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000005'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000005'::uuid;  -- NHR-005 -> Carlos

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000006'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000006'::uuid;  -- TRB-006 -> Andres

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000008'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000008'::uuid;  -- PJK-008 -> Valeria

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000009'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000011'::uuid;  -- NHR-011 -> Nicolas

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000010'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000015'::uuid;  -- NHR-015 -> Laura

UPDATE vehiculos SET conductor_id = 'b0000000-0000-0000-0000-000000000011'::uuid
 WHERE id = 'a0000000-0000-0000-0000-000000000017'::uuid;  -- VNZ-017 -> Patricia

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000001'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000001'::uuid;  -- Juan Carlos -> PJK-001

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000002'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000002'::uuid;  -- Diana -> PJK-002

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000003'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000003'::uuid;  -- Maria -> VNZ-003

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000004'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000004'::uuid;  -- Sergio -> VNZ-004

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000005'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000005'::uuid;  -- Carlos -> NHR-005

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000006'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000006'::uuid;  -- Andres -> TRB-006

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000008'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000008'::uuid;  -- Valeria -> PJK-008

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000011'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000009'::uuid;  -- Nicolas -> NHR-011

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000015'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000010'::uuid;  -- Laura -> NHR-015

UPDATE conductores SET vehiculo_asignado_id = 'a0000000-0000-0000-0000-000000000017'::uuid
 WHERE id = 'b0000000-0000-0000-0000-000000000011'::uuid;  -- Patricia -> VNZ-017


-- =============================================================================
-- 5. VINCULAR usuario driver -> conductor Juan Carlos (ruta R11 EN_TRANSITO)
-- =============================================================================

UPDATE usuarios
   SET conductor_id = 'b0000000-0000-0000-0000-000000000001'::uuid
 WHERE email = 'driver@logisticasm.com';


-- =============================================================================
-- 6. RUTAS (14)
--
-- Zonas unicas para estado CREADA (idx_rutas_zona_creada):
--   R01 d3gpu  R02 d3fy2  R03 d3f74  R04 d3gpx
--
-- Estado           Zona   Tipo   v/c     Ciudad        Paradas
-- R01 CREADA       d3gpu  MOTO   -/-     Santa Marta   3
-- R02 CREADA       d3fy2  VAN    -/-     Barranquilla  4
-- R03 CREADA       d3f74  NHR    -/-     Cartagena     5
-- R04 CREADA       d3gpx  TURBO  -/-     Santa Marta   4
-- R05 LISTA        d3gpy  MOTO   -/-     Santa Marta   4
-- R06 LISTA        d3fy1  VAN    -/-     Barranquilla  5
-- R07 LISTA        d3f71  NHR    -/-     Cartagena     6
-- R08 LISTA        d3f72  TURBO  -/-     Cartagena     4
-- R09 CONFIRMADA   d3fy4  MOTO   V08/C08 Barranquilla  3
-- R10 CONFIRMADA   d3f73  NHR    V11/C09 Cartagena     4
-- R11 EN_TRANSITO  d3gpz  MOTO   V01/C01 Santa Marta   6  (driver user)
-- R12 EN_TRANSITO  d3fy3  MOTO   V02/C02 Barranquilla  5
-- R13 CERRADA_M    d3gpy  VAN    V09/C12 Santa Marta   4  (historica 13-may)
-- R14 CERRADA_A    d3fy1  VAN    V10/C13 Barranquilla  5  (historica 12-may)
-- =============================================================================

INSERT INTO rutas (
    id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
    vehiculo_id, conductor_id,
    fecha_creacion_ruta, fecha_limite_despacho,
    fecha_hora_inicio, fecha_hora_cierre, tipo_cierre, motivo_despacho
)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid,
     'd3gpu', 'CREADA', 18.50, 'MOTO', NULL, NULL,
     '2026-05-15 05:00:00-05', '2026-05-15 20:00:00-05',
     NULL, NULL, NULL, NULL),

    ('d0000000-0000-0000-0000-000000000002'::uuid,
     'd3fy2', 'CREADA', 320.00, 'VAN', NULL, NULL,
     '2026-05-15 05:15:00-05', '2026-05-15 19:00:00-05',
     NULL, NULL, NULL, NULL),

    ('d0000000-0000-0000-0000-000000000003'::uuid,
     'd3f74', 'CREADA', 1450.00, 'NHR', NULL, NULL,
     '2026-05-15 04:45:00-05', '2026-05-15 18:00:00-05',
     NULL, NULL, NULL, NULL),

    ('d0000000-0000-0000-0000-000000000004'::uuid,
     'd3gpx', 'CREADA', 3200.00, 'TURBO', NULL, NULL,
     '2026-05-15 04:30:00-05', '2026-05-16 08:00:00-05',
     NULL, NULL, NULL, NULL),

    ('d0000000-0000-0000-0000-000000000005'::uuid,
     'd3gpy', 'LISTA_PARA_DESPACHO', 28.00, 'MOTO', NULL, NULL,
     '2026-05-15 04:00:00-05', '2026-05-15 18:00:00-05',
     NULL, NULL, NULL, 'PAQUETES_URGENTES'),

    ('d0000000-0000-0000-0000-000000000006'::uuid,
     'd3fy1', 'LISTA_PARA_DESPACHO', 510.00, 'VAN', NULL, NULL,
     '2026-05-15 04:10:00-05', '2026-05-15 17:00:00-05',
     NULL, NULL, NULL, 'CLIENTE_VIP'),

    ('d0000000-0000-0000-0000-000000000007'::uuid,
     'd3f71', 'LISTA_PARA_DESPACHO', 1850.00, 'NHR', NULL, NULL,
     '2026-05-15 03:50:00-05', '2026-05-15 16:00:00-05',
     NULL, NULL, NULL, 'VOLUMEN_ALTO'),

    ('d0000000-0000-0000-0000-000000000008'::uuid,
     'd3f72', 'LISTA_PARA_DESPACHO', 4800.00, 'TURBO', NULL, NULL,
     '2026-05-15 03:30:00-05', '2026-05-16 06:00:00-05',
     NULL, NULL, NULL, 'CARGA_COMPLETA'),

    ('d0000000-0000-0000-0000-000000000009'::uuid,
     'd3fy4', 'CONFIRMADA', 22.00, 'MOTO',
     'a0000000-0000-0000-0000-000000000008'::uuid,
     'b0000000-0000-0000-0000-000000000008'::uuid,
     '2026-05-14 05:00:00-05', '2026-05-15 20:00:00-05',
     NULL, NULL, NULL, 'RUTA_PRIORITARIA'),

    ('d0000000-0000-0000-0000-000000000010'::uuid,
     'd3f73', 'CONFIRMADA', 1100.00, 'NHR',
     'a0000000-0000-0000-0000-000000000011'::uuid,
     'b0000000-0000-0000-0000-000000000009'::uuid,
     '2026-05-14 04:30:00-05', '2026-05-15 18:00:00-05',
     NULL, NULL, NULL, 'DESPACHO_PROGRAMADO'),

    ('d0000000-0000-0000-0000-000000000011'::uuid,
     'd3gpz', 'EN_TRANSITO', 35.00, 'MOTO',
     'a0000000-0000-0000-0000-000000000001'::uuid,
     'b0000000-0000-0000-0000-000000000001'::uuid,
     '2026-05-15 05:00:00-05', '2026-05-15 20:00:00-05',
     '2026-05-15 08:30:00-05', NULL, NULL, 'RUTA_PRIORITARIA'),

    ('d0000000-0000-0000-0000-000000000012'::uuid,
     'd3fy3', 'EN_TRANSITO', 27.50, 'MOTO',
     'a0000000-0000-0000-0000-000000000002'::uuid,
     'b0000000-0000-0000-0000-000000000002'::uuid,
     '2026-05-15 05:10:00-05', '2026-05-15 19:00:00-05',
     '2026-05-15 09:00:00-05', NULL, NULL, 'PAQUETES_URGENTES'),

    ('d0000000-0000-0000-0000-000000000013'::uuid,
     'd3gpy', 'CERRADA_MANUAL', 420.00, 'VAN',
     'a0000000-0000-0000-0000-000000000009'::uuid,
     'b0000000-0000-0000-0000-000000000012'::uuid,
     '2026-05-13 05:00:00-05', '2026-05-13 18:00:00-05',
     '2026-05-13 08:10:00-05', '2026-05-13 16:45:00-05', 'MANUAL', 'DESPACHO_PROGRAMADO'),

    ('d0000000-0000-0000-0000-000000000014'::uuid,
     'd3fy1', 'CERRADA_AUTOMATICA', 680.00, 'VAN',
     'a0000000-0000-0000-0000-000000000010'::uuid,
     'b0000000-0000-0000-0000-000000000013'::uuid,
     '2026-05-12 05:00:00-05', '2026-05-12 18:00:00-05',
     '2026-05-12 08:00:00-05', '2026-05-12 23:00:00-05', 'AUTOMATICO', 'VOLUMEN_ALTO');


-- =============================================================================
-- 7. PARADAS (62 total)
-- Santa Marta ~(11.24,-74.21) | Barranquilla ~(10.97,-74.80) | Cartagena ~(10.42,-75.55)
-- =============================================================================

INSERT INTO paradas (
    id, ruta_id, paquete_id, orden, direccion,
    latitud, longitud, tipo_mercancia, metodo_pago, fecha_limite_entrega,
    estado, motivo_novedad, fecha_hora_gestion,
    firma_receptor_url, foto_evidencia_url, nombre_receptor, origen
)
VALUES

    -- R01: CREADA 3 paradas PENDIENTE (Santa Marta, Taganga, d3gpu) ─────────────
    ('e0000000-0000-0000-0000-000000000001'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid,
     1, 'Calle 14 #3-20, Taganga, Santa Marta',
     11.26800, -74.19000, 'ESTANDAR', 'PREPAGO', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000002'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000002'::uuid,
     2, 'Carrera 2 #12-45, Taganga, Santa Marta',
     11.26500, -74.19300, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000003'::uuid,
     'd0000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000003'::uuid,
     3, 'Av. del Rio #8-60, Taganga, Santa Marta',
     11.26200, -74.19600, 'ESTANDAR', 'PREPAGO', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R02: CREADA 4 paradas PENDIENTE (Barranquilla, La Floresta, d3fy2) ────────
    ('e0000000-0000-0000-0000-000000000004'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid, 'f0000000-0000-0000-0000-000000000004'::uuid,
     1, 'Calle 30 #31-30, La Floresta, Barranquilla',
     10.95500, -74.81000, 'ESTANDAR', 'PREPAGO', '2026-05-15 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000005'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid, 'f0000000-0000-0000-0000-000000000005'::uuid,
     2, 'Carrera 43 #26-40, El Recreo, Barranquilla',
     10.95200, -74.81300, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-15 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000006'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid, 'f0000000-0000-0000-0000-000000000006'::uuid,
     3, 'Av. Cordialidad #8-50, Los Pinos, Barranquilla',
     10.94900, -74.81600, 'ESTANDAR', 'PREPAGO', '2026-05-15 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000007'::uuid,
     'd0000000-0000-0000-0000-000000000002'::uuid, 'f0000000-0000-0000-0000-000000000007'::uuid,
     4, 'Calle 17 #28-15, Las Nieves, Barranquilla',
     10.94600, -74.81900, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-15 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R03: CREADA 5 paradas PENDIENTE (Cartagena, Pie de la Popa, d3f74) ────────
    ('e0000000-0000-0000-0000-000000000008'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid, 'f0000000-0000-0000-0000-000000000008'::uuid,
     1, 'Calle Larga #36-80, Pie de la Popa, Cartagena',
     10.42000, -75.53800, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000009'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid, 'f0000000-0000-0000-0000-000000000009'::uuid,
     2, 'Carrera 22 #29-15, Manga, Cartagena',
     10.41700, -75.53500, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000010'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid, 'f0000000-0000-0000-0000-000000000010'::uuid,
     3, 'Av. Pedro de Heredia #32-60, La Espanola, Cartagena',
     10.41400, -75.53200, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000011'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid, 'f0000000-0000-0000-0000-000000000011'::uuid,
     4, 'Calle 28 #19-25, Tesca, Cartagena',
     10.41100, -75.52900, 'FRAGIL', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000012'::uuid,
     'd0000000-0000-0000-0000-000000000003'::uuid, 'f0000000-0000-0000-0000-000000000012'::uuid,
     5, 'Carrera 17 #25-40, Crespito, Cartagena',
     10.40800, -75.52600, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R04: CREADA 4 paradas PENDIENTE (Santa Marta, TURBO industrial, d3gpx) ────
    ('e0000000-0000-0000-0000-000000000013'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid, 'f0000000-0000-0000-0000-000000000013'::uuid,
     1, 'Zona Industrial #8-120, Mamatoco, Santa Marta',
     11.25100, -74.19800, 'PELIGROSO', 'PREPAGO', '2026-05-16 08:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000014'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid, 'f0000000-0000-0000-0000-000000000014'::uuid,
     2, 'Carrera 20 #34-80, Los Almendros, Santa Marta',
     11.25400, -74.19500, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-16 08:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000015'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid, 'f0000000-0000-0000-0000-000000000015'::uuid,
     3, 'Av. del Ferrocarril #42-15, Curinca, Santa Marta',
     11.25700, -74.19200, 'ESTANDAR', 'PREPAGO', '2026-05-16 08:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000016'::uuid,
     'd0000000-0000-0000-0000-000000000004'::uuid, 'f0000000-0000-0000-0000-000000000016'::uuid,
     4, 'Calle 38 #25-60, El Parque, Santa Marta',
     11.26000, -74.18900, 'PELIGROSO', 'PREPAGO', '2026-05-16 08:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R05: LISTA_PARA_DESPACHO 4 paradas PENDIENTE (Santa Marta MOTO) ───────────
    ('e0000000-0000-0000-0000-000000000017'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid, 'f0000000-0000-0000-0000-000000000017'::uuid,
     1, 'Calle 19 #6-30, El Porvenir, Santa Marta',
     11.24400, -74.20800, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000018'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid, 'f0000000-0000-0000-0000-000000000018'::uuid,
     2, 'Carrera 7 #20-15, Samaria, Santa Marta',
     11.24100, -74.21100, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000019'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid, 'f0000000-0000-0000-0000-000000000019'::uuid,
     3, 'Av. Libertador #22-50, Las Delicias, Santa Marta',
     11.23800, -74.21400, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000020'::uuid,
     'd0000000-0000-0000-0000-000000000005'::uuid, 'f0000000-0000-0000-0000-000000000020'::uuid,
     4, 'Calle 11 #9-70, Nacho Vives, Santa Marta',
     11.23500, -74.21700, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R06: LISTA_PARA_DESPACHO 5 paradas PENDIENTE (Barranquilla VAN) ───────────
    ('e0000000-0000-0000-0000-000000000021'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid, 'f0000000-0000-0000-0000-000000000021'::uuid,
     1, 'Calle 55 #37-20, El Tabor, Barranquilla',
     10.96800, -74.79800, 'ESTANDAR', 'PREPAGO', '2026-05-15 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000022'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid, 'f0000000-0000-0000-0000-000000000022'::uuid,
     2, 'Carrera 38 #52-35, Modelo, Barranquilla',
     10.96500, -74.79500, 'FRAGIL', 'PREPAGO', '2026-05-15 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000023'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid, 'f0000000-0000-0000-0000-000000000023'::uuid,
     3, 'Av. Murillo #45-10, La Manga, Barranquilla',
     10.96200, -74.79200, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000024'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid, 'f0000000-0000-0000-0000-000000000024'::uuid,
     4, 'Calle 49 #33-55, El Rosario, Barranquilla',
     10.95900, -74.78900, 'PELIGROSO', 'PREPAGO', '2026-05-15 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000025'::uuid,
     'd0000000-0000-0000-0000-000000000006'::uuid, 'f0000000-0000-0000-0000-000000000025'::uuid,
     5, 'Carrera 35 #46-80, Simon Bolivar, Barranquilla',
     10.95600, -74.78600, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R07: LISTA_PARA_DESPACHO 6 paradas PENDIENTE (Cartagena NHR) ─────────────
    ('e0000000-0000-0000-0000-000000000026'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000026'::uuid,
     1, 'Calle Real #14-30, Bocagrande, Cartagena',
     10.39400, -75.55400, 'ESTANDAR', 'PREPAGO', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000027'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000027'::uuid,
     2, 'Av. San Martin #12-45, Bocagrande, Cartagena',
     10.39100, -75.55100, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000028'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000028'::uuid,
     3, 'Carrera 3 #10-60, El Laguito, Cartagena',
     10.38800, -75.54800, 'FRAGIL', 'PREPAGO', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000029'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000029'::uuid,
     4, 'Calle 8 #4-25, Castillogrande, Cartagena',
     10.38500, -75.54500, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000030'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000030'::uuid,
     5, 'Av. Boquilla #22-40, El Cabrero, Cartagena',
     10.38200, -75.54200, 'ESTANDAR', 'PREPAGO', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000031'::uuid,
     'd0000000-0000-0000-0000-000000000007'::uuid, 'f0000000-0000-0000-0000-000000000031'::uuid,
     6, 'Carrera 9 #16-55, Pie del Cerro, Cartagena',
     10.37900, -75.53900, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-15 16:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R08: LISTA_PARA_DESPACHO 4 paradas PENDIENTE (Cartagena TURBO industrial) ─
    ('e0000000-0000-0000-0000-000000000032'::uuid,
     'd0000000-0000-0000-0000-000000000008'::uuid, 'f0000000-0000-0000-0000-000000000032'::uuid,
     1, 'Parque Industrial #15-200, Mamonal, Cartagena',
     10.43000, -75.54700, 'PELIGROSO', 'PREPAGO', '2026-05-16 06:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000033'::uuid,
     'd0000000-0000-0000-0000-000000000008'::uuid, 'f0000000-0000-0000-0000-000000000033'::uuid,
     2, 'Av. Crisanto Luque #40-100, Mamonal, Cartagena',
     10.43300, -75.54400, 'ESTANDAR', 'PREPAGO', '2026-05-16 06:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000034'::uuid,
     'd0000000-0000-0000-0000-000000000008'::uuid, 'f0000000-0000-0000-0000-000000000034'::uuid,
     3, 'Calle del Puerto #6-180, Chambacu, Cartagena',
     10.43600, -75.54100, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-16 06:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000035'::uuid,
     'd0000000-0000-0000-0000-000000000008'::uuid, 'f0000000-0000-0000-0000-000000000035'::uuid,
     4, 'Carrera 52 #20-300, Zona Franca, Cartagena',
     10.43900, -75.53800, 'ESTANDAR', 'PREPAGO', '2026-05-16 06:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R09: CONFIRMADA 3 paradas PENDIENTE (Barranquilla MOTO — Valeria/PJK-008) ─
    ('e0000000-0000-0000-0000-000000000036'::uuid,
     'd0000000-0000-0000-0000-000000000009'::uuid, 'f0000000-0000-0000-0000-000000000036'::uuid,
     1, 'Calle 93 #49-20, Villa Country, Barranquilla',
     10.98400, -74.80100, 'FRAGIL', 'PREPAGO', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000037'::uuid,
     'd0000000-0000-0000-0000-000000000009'::uuid, 'f0000000-0000-0000-0000-000000000037'::uuid,
     2, 'Carrera 51 #88-40, Alto Prado, Barranquilla',
     10.98700, -74.79800, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000038'::uuid,
     'd0000000-0000-0000-0000-000000000009'::uuid, 'f0000000-0000-0000-0000-000000000038'::uuid,
     3, 'Av. Circunvalar #79-15, La Cumbre, Barranquilla',
     10.99000, -74.79500, 'ESTANDAR', 'PREPAGO', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R10: CONFIRMADA 4 paradas PENDIENTE (Cartagena NHR — Nicolas/NHR-011) ──────
    ('e0000000-0000-0000-0000-000000000039'::uuid,
     'd0000000-0000-0000-0000-000000000010'::uuid, 'f0000000-0000-0000-0000-000000000039'::uuid,
     1, 'Calle 30 #29-10, Bosque, Cartagena',
     10.42300, -75.54700, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000040'::uuid,
     'd0000000-0000-0000-0000-000000000010'::uuid, 'f0000000-0000-0000-0000-000000000040'::uuid,
     2, 'Carrera 24 #26-35, Manga, Cartagena',
     10.42000, -75.54400, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000041'::uuid,
     'd0000000-0000-0000-0000-000000000010'::uuid, 'f0000000-0000-0000-0000-000000000041'::uuid,
     3, 'Av. del Lago #18-50, El Campestre, Cartagena',
     10.41700, -75.54100, 'ESTANDAR', 'PREPAGO', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000042'::uuid,
     'd0000000-0000-0000-0000-000000000010'::uuid, 'f0000000-0000-0000-0000-000000000042'::uuid,
     4, 'Calle 19 #15-25, San Fernando, Cartagena',
     10.41400, -75.53800, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-15 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R11: EN_TRANSITO 6 paradas mixtas (Santa Marta MOTO — Juan Carlos / driver) ─
    ('e0000000-0000-0000-0000-000000000043'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000043'::uuid,
     1, 'Calle 22 #15-40, Centro Historico, Santa Marta',
     11.24150, -74.20900, 'ESTANDAR', 'PREPAGO', '2026-05-15 20:00:00-05',
     'EXITOSA', NULL, '2026-05-15 08:55:00-05',
     'https://storage.logisticasm.com/firmas/e043.png',
     'https://storage.logisticasm.com/fotos/e043.jpg',
     'Rosa Elena Vargas', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000044'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000044'::uuid,
     2, 'Carrera 5 #18-20, El Prado, Santa Marta',
     11.23900, -74.21200, 'FRAGIL', 'PREPAGO', '2026-05-15 20:00:00-05',
     'EXITOSA', NULL, '2026-05-15 09:25:00-05',
     'https://storage.logisticasm.com/firmas/e044.png',
     'https://storage.logisticasm.com/fotos/e044.jpg',
     'Mauricio Suarez Castro', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000045'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000045'::uuid,
     3, 'Av. del Ferrocarril #30-12, Santa Marta',
     11.24500, -74.20600, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 20:00:00-05',
     'EXITOSA', NULL, '2026-05-15 09:58:00-05',
     'https://storage.logisticasm.com/firmas/e045.png',
     'https://storage.logisticasm.com/fotos/e045.jpg',
     'Luis Fernando Diaz', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000046'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000046'::uuid,
     4, 'Calle 10 #8-55, Gaira, Santa Marta',
     11.24900, -74.20100, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 20:00:00-05',
     'FALLIDA', 'CLIENTE_AUSENTE', '2026-05-15 10:30:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e046.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000047'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000047'::uuid,
     5, 'Carrera 19 #12-80, Mamatoco, Santa Marta',
     11.25100, -74.19800, 'PELIGROSO', 'PREPAGO', '2026-05-15 20:00:00-05',
     'NOVEDAD', 'DIRECCION_INCORRECTA', '2026-05-15 11:05:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e047.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000048'::uuid,
     'd0000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000048'::uuid,
     6, 'Calle 30 #23-10, Los Fundadores, Santa Marta',
     11.24200, -74.21500, 'ESTANDAR', 'PREPAGO', '2026-05-15 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R12: EN_TRANSITO 5 paradas mixtas (Barranquilla MOTO — Diana/PJK-002) ─────
    ('e0000000-0000-0000-0000-000000000049'::uuid,
     'd0000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000049'::uuid,
     1, 'Calle 72 #46-21, El Prado, Barranquilla',
     10.97200, -74.80100, 'ESTANDAR', 'PREPAGO', '2026-05-15 19:00:00-05',
     'EXITOSA', NULL, '2026-05-15 09:20:00-05',
     'https://storage.logisticasm.com/firmas/e049.png',
     'https://storage.logisticasm.com/fotos/e049.jpg',
     'Claudia Ines Ramos', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000050'::uuid,
     'd0000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000050'::uuid,
     2, 'Carrera 53 #68-40, Bellavista, Barranquilla',
     10.96900, -74.79800, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-15 19:00:00-05',
     'EXITOSA', NULL, '2026-05-15 09:55:00-05',
     'https://storage.logisticasm.com/firmas/e050.png',
     'https://storage.logisticasm.com/fotos/e050.jpg',
     'Felipe Andrade Mora', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000051'::uuid,
     'd0000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000051'::uuid,
     3, 'Calle 84 #52-10, Altos del Prado, Barranquilla',
     10.97600, -74.79500, 'ESTANDAR', 'PREPAGO', '2026-05-15 19:00:00-05',
     'FALLIDA', 'RECHAZADO_POR_CLIENTE', '2026-05-15 10:28:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e051.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000052'::uuid,
     'd0000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000052'::uuid,
     4, 'Av. Murillo #34-55, La Victoria, Barranquilla',
     10.96500, -74.80500, 'PELIGROSO', 'PREPAGO', '2026-05-15 19:00:00-05',
     'NOVEDAD', 'ZONA_DIFICIL_ACCESO', '2026-05-15 11:00:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e052.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000053'::uuid,
     'd0000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000053'::uuid,
     5, 'Calle 45 #28-90, Barrio Abajo, Barranquilla',
     10.96100, -74.80900, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-15 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- R13: CERRADA_MANUAL 4 paradas finalizadas (Santa Marta VAN — Roberto) ──────
    ('e0000000-0000-0000-0000-000000000054'::uuid,
     'd0000000-0000-0000-0000-000000000013'::uuid, 'f0000000-0000-0000-0000-000000000054'::uuid,
     1, 'Calle 12 #5-80, Pescaito, Santa Marta',
     11.24300, -74.20700, 'ESTANDAR', 'PREPAGO', '2026-05-13 18:00:00-05',
     'EXITOSA', NULL, '2026-05-13 09:00:00-05',
     'https://storage.logisticasm.com/firmas/e054.png',
     'https://storage.logisticasm.com/fotos/e054.jpg',
     'Hernando Torres Vega', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000055'::uuid,
     'd0000000-0000-0000-0000-000000000013'::uuid, 'f0000000-0000-0000-0000-000000000055'::uuid,
     2, 'Av. Libertador #28-55, Santa Marta',
     11.24700, -74.20300, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-13 18:00:00-05',
     'EXITOSA', NULL, '2026-05-13 09:50:00-05',
     'https://storage.logisticasm.com/firmas/e055.png',
     'https://storage.logisticasm.com/fotos/e055.jpg',
     'Gloria Orozco Perez', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000056'::uuid,
     'd0000000-0000-0000-0000-000000000013'::uuid, 'f0000000-0000-0000-0000-000000000056'::uuid,
     3, 'Calle 20 #17-30, La Candelaria, Santa Marta',
     11.24000, -74.21600, 'ESTANDAR', 'PREPAGO', '2026-05-13 18:00:00-05',
     'FALLIDA', 'CLIENTE_AUSENTE', '2026-05-13 10:30:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e056.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000057'::uuid,
     'd0000000-0000-0000-0000-000000000013'::uuid, 'f0000000-0000-0000-0000-000000000057'::uuid,
     4, 'Carrera 10 #14-60, Los Almendros, Santa Marta',
     11.23900, -74.20800, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-13 18:00:00-05',
     'EXITOSA', NULL, '2026-05-13 11:10:00-05',
     'https://storage.logisticasm.com/firmas/e057.png',
     'https://storage.logisticasm.com/fotos/e057.jpg',
     'Ismael Cure Barros', 'CONDUCTOR'),

    -- R14: CERRADA_AUTOMATICA 5 paradas finalizadas (Barranquilla VAN — Camila) ──
    ('e0000000-0000-0000-0000-000000000058'::uuid,
     'd0000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000058'::uuid,
     1, 'Calle 70 #41-20, Ciudad Jardin, Barranquilla',
     10.97000, -74.80300, 'ESTANDAR', 'PREPAGO', '2026-05-12 18:00:00-05',
     'EXITOSA', NULL, '2026-05-12 09:15:00-05',
     'https://storage.logisticasm.com/firmas/e058.png',
     'https://storage.logisticasm.com/fotos/e058.jpg',
     'Beatriz Salgado Nunez', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000059'::uuid,
     'd0000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000059'::uuid,
     2, 'Carrera 46 #76-50, Los Andes, Barranquilla',
     10.97400, -74.79600, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-12 18:00:00-05',
     'NOVEDAD', 'DAÑADO_EN_RUTA', '2026-05-12 10:00:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e059.jpg',
     NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000060'::uuid,
     'd0000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000060'::uuid,
     3, 'Calle 85 #55-34, San Jose, Barranquilla',
     10.97800, -74.79200, 'ESTANDAR', 'PREPAGO', '2026-05-12 18:00:00-05',
     'EXITOSA', NULL, '2026-05-12 10:50:00-05',
     'https://storage.logisticasm.com/firmas/e060.png',
     'https://storage.logisticasm.com/fotos/e060.jpg',
     'Jairo Bustamante Cruz', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000061'::uuid,
     'd0000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000061'::uuid,
     4, 'Av. Olaya Herrera #63-15, Barranquilla',
     10.96700, -74.80700, 'PELIGROSO', 'CONTRA_ENTREGA', '2026-05-12 18:00:00-05',
     'EXITOSA', NULL, '2026-05-12 11:35:00-05',
     'https://storage.logisticasm.com/firmas/e061.png',
     'https://storage.logisticasm.com/fotos/e061.jpg',
     'Natalia Diaz Quintero', 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000062'::uuid,
     'd0000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000062'::uuid,
     5, 'Carrera 38 #58-22, El Prado, Barranquilla',
     10.97100, -74.79900, 'ESTANDAR', 'PREPAGO', '2026-05-12 18:00:00-05',
     'FALLIDA', 'EXTRAVIADO', '2026-05-12 22:30:00-05',
     NULL, 'https://storage.logisticasm.com/fotos/e062.jpg',
     NULL, 'SISTEMA');
