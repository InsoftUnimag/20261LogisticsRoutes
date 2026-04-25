package com.logistics.routes.infrastructure.dto.request;

import jakarta.validation.constraints.NotNull;

public record CierreRutaRequest(
        @NotNull(message = "confirmarConPendientes es obligatorio")
        Boolean confirmarConPendientes
) {}
