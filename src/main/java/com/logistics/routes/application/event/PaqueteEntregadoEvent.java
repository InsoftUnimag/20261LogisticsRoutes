package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload del evento PAQUETE_ENTREGADO publicado a Módulo 1
 * cuando el conductor registra entrega exitosa con POD (SPEC-08 sección 3, evento 2).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaqueteEntregadoEvent(
        String tipoEvento,
        UUID paqueteId,
        UUID rutaId,
        Instant fechaHoraEvento,
        Evidencia evidencia
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Evidencia(String urlFoto, String urlFirma) {}

    public static PaqueteEntregadoEvent of(UUID paqueteId, UUID rutaId, Instant fechaHora,
                                           String urlFoto, String urlFirma) {
        return new PaqueteEntregadoEvent(
                "PAQUETE_ENTREGADO",
                paqueteId, rutaId, fechaHora,
                new Evidencia(urlFoto, urlFirma));
    }
}
