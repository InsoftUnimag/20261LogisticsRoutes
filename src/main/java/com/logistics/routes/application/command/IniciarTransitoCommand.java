package com.logistics.routes.application.command;

import java.util.UUID;

public record IniciarTransitoCommand(UUID rutaId, UUID conductorId) {}
