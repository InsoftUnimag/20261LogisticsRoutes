package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento RUTA_ASIGNADA publicado a Módulo 1 (SPEC-08 sección 3)
 * cuando M2 asigna exitosamente un paquete recibido vía SOLICITAR_RUTA a una
 * ruta existente o recién creada.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RutaAsignadaEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento
) {
    public static RutaAsignadaEvent of(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        return new RutaAsignadaEvent("RUTA_ASIGNADA", paqueteId, rutaId, fechaHora);
    }
}
