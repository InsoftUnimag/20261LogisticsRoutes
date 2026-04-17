package com.logistics.routes.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Servicio responsable de generar y validar tokens JWT.
 * <p>
 * Algoritmo: HMAC-SHA256 (HS256) usando la librería jjwt 0.12.x.
 * El secret y el tiempo de expiración se leen de application.yml → app.jwt.*
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey  = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera un token JWT firmado con el email del usuario y su rol.
     *
     * @param email email del usuario (subject del token)
     * @param rol   rol del usuario (ej: DISPATCHER, DRIVER, FLEET_ADMIN, SYSTEM)
     * @return token JWT compacto como String
     */
    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el email (subject) del token.
     * Lanza JwtException si el token es inválido o expirado.
     */
    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    /**
     * Extrae el rol del claim personalizado "rol".
     */
    public String extraerRol(String token) {
        return parsearClaims(token).get("rol", String.class);
    }

    /**
     * Valida que el token esté bien formado, firmado correctamente y no expirado.
     *
     * @return true si el token es válido, false en cualquier otro caso
     */
    public boolean esValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Retorna el instante de expiración calculado desde ahora.
     * Útil para incluirlo en la respuesta del login.
     */
    public Instant calcularExpiracion() {
        return Instant.now().plusMillis(expirationMs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interno
    // ─────────────────────────────────────────────────────────────────────────

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
