-- =============================================================================
-- V7__actualizar_zonas_precision5.sql
-- Actualiza zona_operacion de vehículos de precisión 3 a precisión 5.
--
-- El seed V2 usaba geohashes aproximados de 3 caracteres (d2g, d2f, d2b).
-- Los valores correctos computados con ch.hsr.geohash para los centros de
-- cada ciudad son:
--
--   Santa Marta  (11.2408, -74.2110)  → d3gpz   (antes: d2g)
--   Barranquilla (10.9640, -74.7964)  → d3fy3   (antes: d2f)
--   Cartagena    (10.3910, -75.5144)  → d3f71   (antes: d2b)
--
-- Política del equipo: toda zona geográfica usa precisión 5 (~4.9 km × 4.9 km).
-- =============================================================================

UPDATE vehiculos SET zona_operacion = 'd3gpz' WHERE zona_operacion = 'd2g';
UPDATE vehiculos SET zona_operacion = 'd3fy3' WHERE zona_operacion = 'd2f';
UPDATE vehiculos SET zona_operacion = 'd3f71' WHERE zona_operacion = 'd2b';
