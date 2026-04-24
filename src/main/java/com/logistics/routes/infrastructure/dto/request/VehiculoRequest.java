package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.ActualizarVehiculoCommand;
import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.domain.enums.TipoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VehiculoRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}[0-9]{2,3}[A-Z]?$",
                 message = "Formato de placa inválido. Ejemplos válidos: MNT478, BDR921, KLM30A")
        String placa,

        @NotNull
        TipoVehiculo tipo,

        @NotBlank
        @Size(max = 100)
        String modelo,

        @NotNull
        @Positive
        BigDecimal capacidadPesoKg,

        @NotNull
        @Positive
        BigDecimal volumenMaximoM3,

        @NotBlank
        @Size(min = 5, max = 5, message = "La zona de operación debe ser un geohash de exactamente 5 caracteres")
        String zonaOperacion

) {
    public RegistrarVehiculoCommand toRegistrarCommand() {
        return new RegistrarVehiculoCommand(
                placa, tipo, modelo, capacidadPesoKg, volumenMaximoM3, zonaOperacion);
    }

    public ActualizarVehiculoCommand toActualizarCommand() {
        return new ActualizarVehiculoCommand(
                tipo, modelo, capacidadPesoKg, volumenMaximoM3, zonaOperacion);
    }
}
