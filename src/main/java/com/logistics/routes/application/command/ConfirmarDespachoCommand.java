package com.logistics.routes.application.command;

import java.util.UUID;

public record ConfirmarDespachoCommand(
        UUID conductorId,
        UUID vehiculoId
) {}
