-- Vincula el usuario DRIVER seed (driver@logisticasm.com) con el conductor
-- Juan Carlos Martínez (550e8400-...-446655440011) para que la operación de
-- campo del conductor pueda resolver su conductorId desde el JWT.

UPDATE usuarios
SET conductor_id = '550e8400-e29b-41d4-a716-446655440011'::uuid
WHERE email = 'driver@logisticasm.com';
