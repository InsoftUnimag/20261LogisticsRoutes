package com.logistics.routes.infrastructure.adapter.out.persistence;

import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.domain.model.HistorialAsignacion;
import com.logistics.routes.infrastructure.persistence.mapper.HistorialAsignacionMapper;
import com.logistics.routes.infrastructure.persistence.repository.HistorialAsignacionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HistorialAsignacionJpaAdapter implements HistorialAsignacionRepositoryPort {

    private final HistorialAsignacionJpaRepository jpaRepository;

    @Override
    public HistorialAsignacion guardar(HistorialAsignacion historial) {
        return HistorialAsignacionMapper.toDomain(
                jpaRepository.save(HistorialAsignacionMapper.toEntity(historial))
        );
    }

    @Override
    public Optional<HistorialAsignacion> buscarActivoPorConductorId(UUID conductorId) {
        return jpaRepository.findByConductorIdAndFechaHoraFinIsNull(conductorId)
                .map(HistorialAsignacionMapper::toDomain);
    }

    @Override
    public List<HistorialAsignacion> buscarPorConductorId(UUID conductorId) {
        return jpaRepository.findByConductorId(conductorId).stream()
                .map(HistorialAsignacionMapper::toDomain)
                .toList();
    }
}
