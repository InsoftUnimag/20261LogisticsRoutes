package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.ConfirmarDespachoCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmarDespachoRequest(

        @NotNull(message = "El conductorId es obligatorio")
        UUID conductorId,

        @NotNull(message = "El vehiculoId es obligatorio")
        UUID vehiculoId

) {
    public ConfirmarDespachoCommand toCommand() {
        return new ConfirmarDespachoCommand(conductorId, vehiculoId);
    }
}
