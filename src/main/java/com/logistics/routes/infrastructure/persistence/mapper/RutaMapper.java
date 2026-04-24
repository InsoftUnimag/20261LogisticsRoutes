package com.logistics.routes.infrastructure.persistence.mapper;

import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.infrastructure.persistence.entity.RutaEntity;

import java.math.BigDecimal;

public class RutaMapper {

    private RutaMapper() {}

    public static RutaEntity toEntity(Ruta r) {
        return RutaEntity.builder()
                .id(r.getId())
                .zona(r.getZona())
                .estado(r.getEstado())
                .pesoAcumuladoKg(BigDecimal.valueOf(r.getPesoAcumuladoKg()))
                .tipoVehiculoRequerido(r.getTipoVehiculoRequerido())
                .vehiculoId(r.getVehiculoId())
                .conductorId(r.getConductorId())
                .fechaCreacionRuta(r.getFechaCreacionRuta())
                .fechaLimiteDespacho(r.getFechaLimiteDespacho())
                .fechaHoraInicio(r.getFechaHoraInicio())
                .fechaHoraCierre(r.getFechaHoraCierre())
                .tipoCierre(r.getTipoCierre())
                .build();
    }

    public static Ruta toDomain(RutaEntity e) {
        return Ruta.reconstituir(
                e.getId(),
                e.getZona(),
                e.getEstado(),
                e.getPesoAcumuladoKg().doubleValue(),
                e.getTipoVehiculoRequerido(),
                e.getVehiculoId(),
                e.getConductorId(),
                e.getFechaCreacionRuta(),
                e.getFechaLimiteDespacho(),
                e.getFechaHoraInicio(),
                e.getFechaHoraCierre(),
                e.getTipoCierre()
        );
    }
}
