package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.SolicitarRutaCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

        @NotNull(message = "La dirección es obligatoria")
        DireccionDto direccion,

        String tipoMercancia,

        String metodoPago,

        Instant fechaLimiteEntrega

) {
    // M1 envía direccion como objeto anidado; solo extraemos el campo texto
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DireccionDto(String direccion) {}

    public SolicitarRutaCommand toCommand() {
        return new SolicitarRutaCommand(
                paqueteId, pesoKg, latitud, longitud,
                direccion.direccion(), tipoMercancia, metodoPago, fechaLimiteEntrega
        );
    }
}
