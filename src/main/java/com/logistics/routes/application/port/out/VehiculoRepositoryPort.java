package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.model.Vehiculo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehiculoRepositoryPort {
    Vehiculo guardar(Vehiculo vehiculo);
    Optional<Vehiculo> buscarPorId(UUID id);
    boolean existePorPlaca(String placa);
    List<Vehiculo> buscarTodos();
}
