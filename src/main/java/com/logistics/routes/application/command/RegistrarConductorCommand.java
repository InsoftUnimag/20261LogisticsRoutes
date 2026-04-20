package com.logistics.routes.application.command;

import com.logistics.routes.domain.enums.ModeloContrato;

public record RegistrarConductorCommand(
        String nombre,
        String email,
        ModeloContrato modeloContrato
) {}
