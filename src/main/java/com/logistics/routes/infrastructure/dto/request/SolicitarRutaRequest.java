package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.SolicitarRutaCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record SolicitarRutaRequest(

        @NotNull(message = "El paqueteId es obligatorio")
        UUID paqueteId,

        @NotNull(message = "El peso es obligatorio")
        @Positive(message = "El peso debe ser mayor a 0")
        Double pesoKg,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        Double longitud,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 500, message = "La dirección no puede superar los 500 caracteres")
        String direccion,

        @Size(max = 20, message = "El tipo de mercancía no puede superar los 20 caracteres")
        String tipoMercancia,

        @Size(max = 20, message = "El método de pago no puede superar los 20 caracteres")
        String metodoPago,

        Instant fechaLimiteEntrega

) {
    public SolicitarRutaCommand toCommand() {
        return new SolicitarRutaCommand(
                paqueteId, pesoKg, latitud, longitud,
                direccion, tipoMercancia, metodoPago, fechaLimiteEntrega
        );
    }
}
