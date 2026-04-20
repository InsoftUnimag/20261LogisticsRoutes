package com.logistics.routes.infrastructure.adapter.out.persistence;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.infrastructure.persistence.mapper.ConductorMapper;
import com.logistics.routes.infrastructure.persistence.repository.ConductorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConductorJpaAdapter implements ConductorRepositoryPort {

    private final ConductorJpaRepository jpaRepository;

    @Override
    public Conductor guardar(Conductor conductor) {
        return ConductorMapper.toDomain(
                jpaRepository.save(ConductorMapper.toEntity(conductor))
        );
    }

    @Override
    public Optional<Conductor> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(ConductorMapper::toDomain);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<Conductor> buscarTodos() {
        return jpaRepository.findAll().stream()
                .map(ConductorMapper::toDomain)
                .toList();
    }
}
