package com.logistics.routes.application.port.in;

import com.logistics.routes.application.command.RegistrarConductorCommand;
import com.logistics.routes.domain.model.Conductor;

public interface RegistrarConductorPort {
    Conductor ejecutar(RegistrarConductorCommand command);
}
