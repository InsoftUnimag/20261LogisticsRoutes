package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento PARADA_FALLIDA publicado a Módulo 1
 * cuando el conductor registra una parada fallida (SPEC-08 sección 3, evento 3).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ParadaFallidaEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento,
        String motivo
) {
    public static ParadaFallidaEvent of(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora) {
        return new ParadaFallidaEvent("PARADA_FALLIDA", paqueteId, rutaId, fechaHora, motivo);
    }
}
