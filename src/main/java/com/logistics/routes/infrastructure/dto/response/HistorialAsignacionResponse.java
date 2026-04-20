package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.model.HistorialAsignacion;

import java.time.Instant;
import java.util.UUID;

public record HistorialAsignacionResponse(
        UUID id,
        UUID conductorId,
        UUID vehiculoId,
        Instant fechaHoraInicio,
        Instant fechaHoraFin,
        boolean activo
) {
    public static HistorialAsignacionResponse from(HistorialAsignacion h) {
        return new HistorialAsignacionResponse(
                h.getId(),
                h.getConductorId(),
                h.getVehiculoId(),
                h.getFechaInicio(),
                h.getFechaFin(),
                h.isActivo()
        );
    }
}
