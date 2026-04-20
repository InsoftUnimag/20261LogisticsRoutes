package com.logistics.routes.infrastructure.persistence.mapper;

import com.logistics.routes.domain.model.HistorialAsignacion;
import com.logistics.routes.infrastructure.persistence.entity.HistorialAsignacionEntity;

public class HistorialAsignacionMapper {

    private HistorialAsignacionMapper() {}

    public static HistorialAsignacionEntity toEntity(HistorialAsignacion h) {
        return HistorialAsignacionEntity.builder()
                .id(h.getId())
                .conductorId(h.getConductorId())
                .vehiculoId(h.getVehiculoId())
                .fechaHoraInicio(h.getFechaInicio())
                .fechaHoraFin(h.getFechaFin())
                .build();
    }

    public static HistorialAsignacion toDomain(HistorialAsignacionEntity e) {
        return HistorialAsignacion.reconstituir(
                e.getId(),
                e.getConductorId(),
                e.getVehiculoId(),
                e.getFechaHoraInicio(),
                e.getFechaHoraFin()
        );
    }
}
