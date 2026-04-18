package com.logistics.routes.infrastructure.persistence.mapper;

import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.infrastructure.persistence.entity.VehiculoEntity;

import java.math.BigDecimal;

public class VehiculoMapper {

    private VehiculoMapper() {}

    public static VehiculoEntity toEntity(Vehiculo v) {
        return VehiculoEntity.builder()
                .id(v.getId())
                .placa(v.getPlaca())
                .tipo(v.getTipo())
                .modelo(v.getModelo())
                .capacidadPesoKg(BigDecimal.valueOf(v.getCapacidadPesoKg()))
                .volumenMaximoM3(BigDecimal.valueOf(v.getVolumenMaximoM3()))
                .zonaOperacion(v.getZonaOperacion())
                .estado(v.getEstado())
                .conductorId(v.getConductorId())
                .build();
    }

    public static Vehiculo toDomain(VehiculoEntity e) {
        return Vehiculo.reconstituir(
                e.getId(),
                e.getPlaca(),
                e.getTipo(),
                e.getModelo(),
                e.getCapacidadPesoKg().doubleValue(),
                e.getVolumenMaximoM3().doubleValue(),
                e.getZonaOperacion(),
                e.getEstado(),
                e.getConductorId()
        );
    }
}
