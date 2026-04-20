package com.logistics.routes.application.port.in;

import com.logistics.routes.domain.model.HistorialAsignacion;

import java.util.List;
import java.util.UUID;

public interface ConsultarHistorialConductorPort {
    List<HistorialAsignacion> ejecutar(UUID conductorId);
}
