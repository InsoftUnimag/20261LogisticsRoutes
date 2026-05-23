-- =============================================================================
-- V13__rutas_moto_lista_despacho.sql
--
-- Agrega 5 rutas LISTA_PARA_DESPACHO de tipo MOTO con sus paradas PENDIENTE.
-- Útil para demo del flujo despachador: asignar vehículo/conductor y confirmar.
--
-- No borra nada — solo inserta sobre el escenario de V12.
--
-- UUID prefijos (continúan desde V12):
--   d0...015 → d0...019  rutas
--   e0...063 → e0...077  paradas
--   f0...063 → f0...077  paquetes (IDs externos)
--
-- Zonas de referencia:
--   Santa Marta   ≈ (11.24, -74.21)  geohash d3gpz
--   Barranquilla  ≈ (10.96, -74.80)  geohash d3fy3
--   Cartagena     ≈ (10.42, -75.55)  geohash d3f71
-- =============================================================================


INSERT INTO rutas (
    id, zona, estado, peso_acumulado_kg, tipo_vehiculo_requerido,
    vehiculo_id, conductor_id,
    fecha_creacion_ruta, fecha_limite_despacho,
    fecha_hora_inicio, fecha_hora_cierre, tipo_cierre, motivo_despacho
)
VALUES
    -- R15: Santa Marta — 3 paquetes pequeños
    ('d0000000-0000-0000-0000-000000000015'::uuid,
     'd3gpz', 'LISTA_PARA_DESPACHO', 27.50, 'MOTO',
     NULL, NULL,
     '2026-05-23 05:30:00-05', '2026-05-23 18:00:00-05',
     NULL, NULL, NULL, 'PAQUETES_URGENTES'),

    -- R16: Barranquilla — 3 paquetes ligeros
    ('d0000000-0000-0000-0000-000000000016'::uuid,
     'd3fy3', 'LISTA_PARA_DESPACHO', 33.00, 'MOTO',
     NULL, NULL,
     '2026-05-23 06:00:00-05', '2026-05-23 20:00:00-05',
     NULL, NULL, NULL, 'CLIENTE_VIP'),

    -- R17: Santa Marta zona norte — 3 paquetes
    ('d0000000-0000-0000-0000-000000000017'::uuid,
     'd3gpy', 'LISTA_PARA_DESPACHO', 21.00, 'MOTO',
     NULL, NULL,
     '2026-05-23 06:15:00-05', '2026-05-23 19:00:00-05',
     NULL, NULL, NULL, 'RUTA_PRIORITARIA'),

    -- R18: Cartagena — 4 paquetes pequeños
    ('d0000000-0000-0000-0000-000000000018'::uuid,
     'd3f71', 'LISTA_PARA_DESPACHO', 39.50, 'MOTO',
     NULL, NULL,
     '2026-05-23 05:45:00-05', '2026-05-23 17:00:00-05',
     NULL, NULL, NULL, 'DESPACHO_PROGRAMADO'),

    -- R19: Barranquilla zona sur — 2 paquetes
    ('d0000000-0000-0000-0000-000000000019'::uuid,
     'd3fy1', 'LISTA_PARA_DESPACHO', 17.00, 'MOTO',
     NULL, NULL,
     '2026-05-23 06:30:00-05', '2026-05-23 21:00:00-05',
     NULL, NULL, NULL, 'CARGA_COMPLETA');


INSERT INTO paradas (
    id, ruta_id, paquete_id, orden, direccion,
    latitud, longitud, tipo_mercancia, metodo_pago, fecha_limite_entrega,
    estado, motivo_novedad, fecha_hora_gestion,
    firma_receptor_url, foto_evidencia_url, nombre_receptor, origen
)
VALUES

    -- ── R15: Santa Marta d3gpz (3 paradas) ──────────────────────────────────

    ('e0000000-0000-0000-0000-000000000063'::uuid,
     'd0000000-0000-0000-0000-000000000015'::uuid,
     'f0000000-0000-0000-0000-000000000063'::uuid,
     1, 'Calle 22 #5-40, Centro Histórico, Santa Marta',
     11.24150, -74.20900, 'ESTANDAR', 'PREPAGO', '2026-05-23 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000064'::uuid,
     'd0000000-0000-0000-0000-000000000015'::uuid,
     'f0000000-0000-0000-0000-000000000064'::uuid,
     2, 'Carrera 4 #14-20, El Prado, Santa Marta',
     11.23800, -74.21200, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-23 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000065'::uuid,
     'd0000000-0000-0000-0000-000000000015'::uuid,
     'f0000000-0000-0000-0000-000000000065'::uuid,
     3, 'Av. del Ferrocarril #30-55, Santa Marta',
     11.24500, -74.20600, 'ESTANDAR', 'PREPAGO', '2026-05-23 18:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ── R16: Barranquilla d3fy3 (3 paradas) ─────────────────────────────────

    ('e0000000-0000-0000-0000-000000000066'::uuid,
     'd0000000-0000-0000-0000-000000000016'::uuid,
     'f0000000-0000-0000-0000-000000000066'::uuid,
     1, 'Calle 72 #46-21, El Prado, Barranquilla',
     10.97200, -74.80100, 'ESTANDAR', 'PREPAGO', '2026-05-23 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000067'::uuid,
     'd0000000-0000-0000-0000-000000000016'::uuid,
     'f0000000-0000-0000-0000-000000000067'::uuid,
     2, 'Carrera 53 #68-40, Bellavista, Barranquilla',
     10.96900, -74.79800, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-23 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000068'::uuid,
     'd0000000-0000-0000-0000-000000000016'::uuid,
     'f0000000-0000-0000-0000-000000000068'::uuid,
     3, 'Calle 84 #52-10, Altos del Prado, Barranquilla',
     10.97600, -74.79500, 'FRAGIL', 'PREPAGO', '2026-05-23 20:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ── R17: Santa Marta norte d3gpy (3 paradas) ────────────────────────────

    ('e0000000-0000-0000-0000-000000000069'::uuid,
     'd0000000-0000-0000-0000-000000000017'::uuid,
     'f0000000-0000-0000-0000-000000000069'::uuid,
     1, 'Calle 10 #8-55, Gaira, Santa Marta',
     11.24900, -74.20100, 'ESTANDAR', 'PREPAGO', '2026-05-23 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000070'::uuid,
     'd0000000-0000-0000-0000-000000000017'::uuid,
     'f0000000-0000-0000-0000-000000000070'::uuid,
     2, 'Calle 30 #23-10, Los Fundadores, Santa Marta',
     11.24200, -74.21500, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-23 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000071'::uuid,
     'd0000000-0000-0000-0000-000000000017'::uuid,
     'f0000000-0000-0000-0000-000000000071'::uuid,
     3, 'Carrera 19 #12-80, Mamatoco, Santa Marta',
     11.25100, -74.19800, 'FRAGIL', 'PREPAGO', '2026-05-23 19:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ── R18: Cartagena d3f71 (4 paradas) ────────────────────────────────────

    ('e0000000-0000-0000-0000-000000000072'::uuid,
     'd0000000-0000-0000-0000-000000000018'::uuid,
     'f0000000-0000-0000-0000-000000000072'::uuid,
     1, 'Calle del Arsenal #8-141, Getsemaní, Cartagena',
     10.42100, -75.55000, 'ESTANDAR', 'PREPAGO', '2026-05-23 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000073'::uuid,
     'd0000000-0000-0000-0000-000000000018'::uuid,
     'f0000000-0000-0000-0000-000000000073'::uuid,
     2, 'Av. del Lago #30-60, Manga, Cartagena',
     10.41700, -75.54600, 'FRAGIL', 'CONTRA_ENTREGA', '2026-05-23 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000074'::uuid,
     'd0000000-0000-0000-0000-000000000018'::uuid,
     'f0000000-0000-0000-0000-000000000074'::uuid,
     3, 'Calle Larga #10-25, San Diego, Cartagena',
     10.42500, -75.54200, 'ESTANDAR', 'PREPAGO', '2026-05-23 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000075'::uuid,
     'd0000000-0000-0000-0000-000000000018'::uuid,
     'f0000000-0000-0000-0000-000000000075'::uuid,
     4, 'Calle 32 #22-10, Bocagrande, Cartagena',
     10.39500, -75.55500, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-23 17:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    -- ── R19: Barranquilla sur d3fy1 (2 paradas) ─────────────────────────────

    ('e0000000-0000-0000-0000-000000000076'::uuid,
     'd0000000-0000-0000-0000-000000000019'::uuid,
     'f0000000-0000-0000-0000-000000000076'::uuid,
     1, 'Av. Murillo #34-55, La Victoria, Barranquilla',
     10.96500, -74.80500, 'ESTANDAR', 'PREPAGO', '2026-05-23 21:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR'),

    ('e0000000-0000-0000-0000-000000000077'::uuid,
     'd0000000-0000-0000-0000-000000000019'::uuid,
     'f0000000-0000-0000-0000-000000000077'::uuid,
     2, 'Calle 45 #28-90, Barrio Abajo, Barranquilla',
     10.96100, -74.80900, 'ESTANDAR', 'CONTRA_ENTREGA', '2026-05-23 21:00:00-05',
     'PENDIENTE', NULL, NULL, NULL, NULL, NULL, 'CONDUCTOR');
