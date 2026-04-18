package com.logistics.routes.application.port.in;

import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.domain.model.Vehiculo;

public interface RegistrarVehiculoPort {
    Vehiculo ejecutar(RegistrarVehiculoCommand command);
}
