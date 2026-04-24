package com.logistics.routes.infrastructure.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmarRutaRequest(

        @NotNull(message = "El conductorId es obligatorio")
        UUID conductorId,

        @NotNull(message = "El vehiculoId es obligatorio")
        UUID vehiculoId

) {}
