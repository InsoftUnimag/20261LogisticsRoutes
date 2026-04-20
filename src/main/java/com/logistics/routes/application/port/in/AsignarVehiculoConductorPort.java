package com.logistics.routes.application.port.in;

import com.logistics.routes.domain.model.Conductor;

import java.util.UUID;

public interface AsignarVehiculoConductorPort {
    Conductor ejecutar(UUID conductorId, UUID vehiculoId);
}
