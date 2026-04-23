package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.model.Conductor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConductorRepositoryPort {
    Conductor guardar(Conductor conductor);
    Optional<Conductor> buscarPorId(UUID id);
    boolean existePorEmail(String email);
    List<Conductor> buscarTodos();
}
