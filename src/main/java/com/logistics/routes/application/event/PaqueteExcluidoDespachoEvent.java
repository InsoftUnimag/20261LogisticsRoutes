package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento PAQUETE_EXCLUIDO_DESPACHO publicado a Módulo 1
 * cuando el Despachador excluye un paquete antes de confirmar la ruta
 * (SPEC-08 sección 3, evento 6).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaqueteExcluidoDespachoEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento
) {
    public static PaqueteExcluidoDespachoEvent of(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        return new PaqueteExcluidoDespachoEvent("PAQUETE_EXCLUIDO_DESPACHO", paqueteId, rutaId, fechaHora);
    }
}
