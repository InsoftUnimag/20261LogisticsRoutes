-- Limpia seeds previos (V4 con hashes rotos, V5 usuario de emergencia)
-- Re-siembra usuarios base usando el hash BCrypt de V5 que ya fue
-- validado contra Spring Security. Password en claro: password123.

DELETE FROM usuarios;

INSERT INTO usuarios (id, email, password_hash, rol, created_at) VALUES
    (gen_random_uuid(), 'admin@logisticasm.com',      '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'FLEET_ADMIN', NOW()),
    (gen_random_uuid(), 'dispatcher@logisticasm.com', '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'DISPATCHER',  NOW()),
    (gen_random_uuid(), 'driver@logisticasm.com',     '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'DRIVER',      NOW()),
    (gen_random_uuid(), 'system@logisticasm.com',     '$2a$10$GDYLpAx2kzPv9dWG4XE8NupmeMCM0wQBz5ZGe9O1k6KPYiYgpOJbq', 'SYSTEM',      NOW());
