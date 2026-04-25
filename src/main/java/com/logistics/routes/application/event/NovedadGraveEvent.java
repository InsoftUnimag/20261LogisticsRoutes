package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento NOVEDAD_GRAVE publicado a Módulo 1
 * cuando el conductor registra paquete dañado, extraviado o devolución
 * (SPEC-08 sección 3, evento 4).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NovedadGraveEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento,
        String tipoNovedad
) {
    public static NovedadGraveEvent of(UUID paqueteId, UUID rutaId, String tipoNovedad, Instant fechaHora) {
        return new NovedadGraveEvent("NOVEDAD_GRAVE", paqueteId, rutaId, fechaHora, tipoNovedad);
    }
}
