package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento PAQUETE_EN_TRANSITO publicado a Módulo 1
 * cuando el conductor confirma el inicio del tránsito (SPEC-08 sección 3, evento 1).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaqueteEnTransitoEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento
) {
    public static PaqueteEnTransitoEvent of(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        return new PaqueteEnTransitoEvent("PAQUETE_EN_TRANSITO", paqueteId, rutaId, fechaHora);
    }
}
