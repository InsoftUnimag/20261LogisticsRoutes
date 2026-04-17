package com.logistics.routes.infrastructure.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Estructura uniforme para todas las respuestas de error de la API.
 * Los campos timestamp y detalles permiten al cliente correlacionar el error
 * y al equipo de soporte reproducirlo con precisión.
 */
public record ErrorResponse(
        String codigo,
        String mensaje,
        Instant timestamp,
        List<String> detalles
) {

    /** Constructor de conveniencia para errores sin detalles adicionales. */
    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, Instant.now(), List.of());
    }

    /** Constructor de conveniencia para errores con lista de detalles (ej. validaciones). */
    public static ErrorResponse of(String codigo, String mensaje, List<String> detalles) {
        return new ErrorResponse(codigo, mensaje, Instant.now(), detalles);
    }
}
