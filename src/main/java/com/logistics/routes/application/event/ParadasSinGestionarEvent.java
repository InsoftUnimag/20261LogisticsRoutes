package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.logistics.routes.domain.enums.TipoCierre;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload del evento PARADAS_SIN_GESTIONAR publicado a Módulo 1
 * cuando una ruta se cierra dejando paradas pendientes (SPEC-08 sección 3, evento 5).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ParadasSinGestionarEvent(
        String tipoEvento,
        UUID rutaId,
        String tipoCierre,
        Instant fechaHoraEvento,
        List<PaqueteRef> paquetes
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaqueteRef(UUID paqueteId) {}

    public static ParadasSinGestionarEvent of(UUID rutaId, TipoCierre tipoCierre,
                                              List<UUID> paqueteIds, Instant fechaHora) {
        List<PaqueteRef> refs = paqueteIds.stream().map(PaqueteRef::new).toList();
        return new ParadasSinGestionarEvent(
                "PARADAS_SIN_GESTIONAR", rutaId, tipoCierre.name(), fechaHora, refs);
    }
}
