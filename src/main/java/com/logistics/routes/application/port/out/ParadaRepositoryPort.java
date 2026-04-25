package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.model.Parada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParadaRepositoryPort {

    Parada guardar(Parada parada);

    List<Parada> guardarTodas(List<Parada> paradas);

    List<Parada> buscarPorRutaId(UUID rutaId);

    Optional<Parada> buscarPorRutaYPaquete(UUID rutaId, UUID paqueteId);

    Optional<Parada> buscarPorId(UUID paradaId);
}
