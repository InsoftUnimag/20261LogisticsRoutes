package com.logistics.routes.infrastructure.persistence.mapper;

import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.infrastructure.persistence.entity.ConductorEntity;

public class ConductorMapper {

    private ConductorMapper() {}

    public static ConductorEntity toEntity(Conductor c) {
        return ConductorEntity.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .email(c.getEmail())
                .modeloContrato(c.getModeloContrato())
                .estado(c.getEstado())
                .vehiculoAsignadoId(c.getVehiculoAsignadoId())
                .build();
    }

    public static Conductor toDomain(ConductorEntity e) {
        return Conductor.reconstituir(
                e.getId(),
                e.getNombre(),
                e.getEmail(),
                e.getModeloContrato(),
                e.getEstado(),
                e.getVehiculoAsignadoId()
        );
    }
}
