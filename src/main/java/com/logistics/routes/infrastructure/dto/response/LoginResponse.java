package com.logistics.routes.infrastructure.dto.response;

import java.time.Instant;

/**
 * DTO de respuesta para el endpoint POST /api/auth/login.
 *
 * @param token     JWT compacto firmado con HMAC-SHA256
 * @param rol       Rol del usuario (DISPATCHER | DRIVER | FLEET_ADMIN | SYSTEM)
 * @param expiracion Instante UTC en que el token expirará
 */
public record LoginResponse(
        String token,
        String rol,
        Instant expiracion
) {}
