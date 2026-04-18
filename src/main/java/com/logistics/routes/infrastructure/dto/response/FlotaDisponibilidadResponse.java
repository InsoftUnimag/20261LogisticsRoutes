package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Vehiculo;

import java.math.BigDecimal;
import java.util.UUID;

public record FlotaDisponibilidadResponse(
        UUID id,
        String placa,
        TipoVehiculo tipo,
        String modelo,
        BigDecimal capacidadPesoKg,
        BigDecimal volumenMaximoM3,
        String zonaOperacion,
        EstadoVehiculo estado,
        UUID conductorId,
        boolean disponibleParaPlanificacion
) {
    public static FlotaDisponibilidadResponse from(Vehiculo v) {
        boolean disponible = v.getEstado() == EstadoVehiculo.DISPONIBLE
                && v.getConductorId() != null;
        return new FlotaDisponibilidadResponse(
                v.getId(),
                v.getPlaca(),
                v.getTipo(),
                v.getModelo(),
                BigDecimal.valueOf(v.getCapacidadPesoKg()),
                BigDecimal.valueOf(v.getVolumenMaximoM3()),
                v.getZonaOperacion(),
                v.getEstado(),
                v.getConductorId(),
                disponible
        );
    }
}
