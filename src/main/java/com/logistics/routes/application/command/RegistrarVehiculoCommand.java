package com.logistics.routes.application.command;

import com.logistics.routes.domain.enums.TipoVehiculo;

import java.math.BigDecimal;

public record RegistrarVehiculoCommand(
        String placa,
        TipoVehiculo tipo,
        String modelo,
        BigDecimal capacidadPesoKg,
        BigDecimal volumenMaximoM3,
        String zonaOperacion          // geohash precisión 3, ej. "d2g"
) {}
