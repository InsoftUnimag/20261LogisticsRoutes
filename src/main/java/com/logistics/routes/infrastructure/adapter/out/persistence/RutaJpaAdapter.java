package com.logistics.routes.infrastructure.adapter.out.persistence;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import com.logistics.routes.infrastructure.persistence.mapper.RutaMapper;
import com.logistics.routes.infrastructure.persistence.repository.RutaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RutaJpaAdapter implements RutaRepositoryPort {

    private static final Set<EstadoRuta> ESTADOS_ACTIVOS =
            Set.of(EstadoRuta.CONFIRMADA, EstadoRuta.EN_TRANSITO);

    private final RutaJpaRepository jpaRepository;

    @Override
    public Ruta guardar(Ruta ruta) {
        return RutaMapper.toDomain(
                jpaRepository.save(RutaMapper.toEntity(ruta))
        );
    }

    @Override
    public Optional<Ruta> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(RutaMapper::toDomain);
    }

    @Override
    public Optional<Ruta> buscarRutaActivaPorZona(ZonaGeografica zona) {
        return jpaRepository
                .findByZonaAndEstado(zona.hash(), EstadoRuta.CREADA)
                .map(RutaMapper::toDomain);
    }

    @Override
    public List<Ruta> buscarPorEstado(EstadoRuta estado) {
        return jpaRepository.findByEstado(estado).stream()
                .map(RutaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Ruta> buscarRutasVencidas(Instant ahora) {
        return jpaRepository
                .findByEstadoAndFechaLimiteDespachoLessThanEqual(EstadoRuta.CREADA, ahora)
                .stream()
                .map(RutaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Ruta> buscarRutasEnTransitoExcedidas(Instant limite) {
        return jpaRepository
                .findByEstadoAndFechaHoraInicioLessThanEqual(EstadoRuta.EN_TRANSITO, limite)
                .stream()
                .map(RutaMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Ruta> buscarRutaActivaPorConductorId(UUID conductorId) {
        return jpaRepository
                .findFirstByConductorIdAndEstadoIn(conductorId, ESTADOS_ACTIVOS)
                .map(RutaMapper::toDomain);
    }
}
