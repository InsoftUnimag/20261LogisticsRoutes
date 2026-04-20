package com.logistics.routes.application.command;

import com.logistics.routes.domain.enums.TipoVehiculo;

import java.math.BigDecimal;

// La placa es inmutable en negocio — no se incluye.
// Para cambiar placa: dar de baja el vehículo y registrar uno nuevo.
public record ActualizarVehiculoCommand(
        TipoVehiculo tipo,
        String modelo,
        BigDecimal capacidadPesoKg,
        BigDecimal volumenMaximoM3,
        String zonaOperacion
) {}
