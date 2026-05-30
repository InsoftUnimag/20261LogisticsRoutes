-- =============================================================================
-- V11__ajustes_usuarios_demo.sql
--
-- 1. Elimina el usuario system@logisticasm.com (no se usa en el frontend).
-- 2. Garantiza que driver@logisticasm.com esté vinculado al conductor
--    Juan Carlos Martínez (b0...001), quien tiene la ruta R04 EN_TRANSITO
--    con 6 paradas en estados variados para pruebas del flujo de campo.
-- =============================================================================

-- 1. Eliminar usuario SYSTEM
DELETE FROM usuarios WHERE email = 'system@logisticasm.com';

-- 2. Asegurar vínculo driver → conductor Juan Carlos → ruta R04 EN_TRANSITO
UPDATE usuarios
   SET conductor_id = 'b0000000-0000-0000-0000-000000000001'::uuid
 WHERE email = 'driver@logisticasm.com';