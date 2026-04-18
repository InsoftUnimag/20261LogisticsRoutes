package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Vehiculo;

import java.math.BigDecimal;
import java.util.UUID;

public record VehiculoResponse(
        UUID id,
        String placa,
        TipoVehiculo tipo,
        String modelo,
        BigDecimal capacidadPesoKg,
        BigDecimal volumenMaximoM3,
        String zonaOperacion,
        EstadoVehiculo estado,
        UUID conductorId
) {
    public static VehiculoResponse from(Vehiculo v) {
        return new VehiculoResponse(
                v.getId(),
                v.getPlaca(),
                v.getTipo(),
                v.getModelo(),
                BigDecimal.valueOf(v.getCapacidadPesoKg()),
                BigDecimal.valueOf(v.getVolumenMaximoM3()),
                v.getZonaOperacion(),
                v.getEstado(),
                v.getConductorId()
        );
    }
}
