package com.logistics.routes.application.port.in;

import com.logistics.routes.application.command.ActualizarVehiculoCommand;
import com.logistics.routes.domain.model.Vehiculo;

import java.util.UUID;

public interface ActualizarVehiculoPort {
    Vehiculo ejecutar(UUID id, ActualizarVehiculoCommand command);
}
