package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.valueobject.ZonaGeografica;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaRepositoryPort {

    Ruta guardar(Ruta ruta);

    Optional<Ruta> buscarPorId(UUID id);

    Optional<Ruta> buscarRutaActivaPorZona(ZonaGeografica zona);

    List<Ruta> buscarPorEstado(EstadoRuta estado);

    List<Ruta> buscarRutasVencidas(Instant ahora);

    List<Ruta> buscarRutasEnTransitoExcedidas(Instant limite);

    Optional<Ruta> buscarRutaActivaPorConductorId(UUID conductorId);
}
