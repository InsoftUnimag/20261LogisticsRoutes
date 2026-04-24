package com.logistics.routes.infrastructure.persistence.mapper;

import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.infrastructure.persistence.entity.ParadaEntity;

import java.math.BigDecimal;

public class ParadaMapper {

    private ParadaMapper() {}

    public static ParadaEntity toEntity(Parada p) {
        return ParadaEntity.builder()
                .id(p.getId())
                .rutaId(p.getRutaId())
                .paqueteId(p.getPaqueteId())
                .orden(p.getOrden())
                .direccion(p.getDireccion())
                .latitud(BigDecimal.valueOf(p.getLatitud()))
                .longitud(BigDecimal.valueOf(p.getLongitud()))
                .tipoMercancia(p.getTipoMercancia())
                .metodoPago(p.getMetodoPago())
                .fechaLimiteEntrega(p.getFechaLimiteEntrega())
                .estado(p.getEstado())
                .origen(p.getOrigen())
                .build();
    }

    public static Parada toDomain(ParadaEntity e) {
        return Parada.reconstituir(
                e.getId(),
                e.getRutaId(),
                e.getPaqueteId(),
                e.getOrden(),
                e.getDireccion(),
                e.getLatitud().doubleValue(),
                e.getLongitud().doubleValue(),
                e.getTipoMercancia(),
                e.getMetodoPago(),
                e.getFechaLimiteEntrega(),
                e.getEstado(),
                e.getOrigen()
        );
    }
}
