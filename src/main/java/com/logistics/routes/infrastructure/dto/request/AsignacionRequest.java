package com.logistics.routes.infrastructure.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AsignacionRequest(

        @NotNull(message = "El vehiculoId es obligatorio")
        UUID vehiculoId

) {}
