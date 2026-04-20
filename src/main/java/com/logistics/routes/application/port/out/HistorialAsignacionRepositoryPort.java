package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.model.HistorialAsignacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialAsignacionRepositoryPort {
    HistorialAsignacion guardar(HistorialAsignacion historial);
    Optional<HistorialAsignacion> buscarActivoPorConductorId(UUID conductorId);
    List<HistorialAsignacion> buscarPorConductorId(UUID conductorId);
}
