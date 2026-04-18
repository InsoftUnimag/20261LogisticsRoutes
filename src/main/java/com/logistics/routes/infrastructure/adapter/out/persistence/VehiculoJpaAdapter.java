package com.logistics.routes.infrastructure.adapter.out.persistence;

import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.infrastructure.persistence.mapper.VehiculoMapper;
import com.logistics.routes.infrastructure.persistence.repository.VehiculoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VehiculoJpaAdapter implements VehiculoRepositoryPort {

    private final VehiculoJpaRepository jpaRepository;

    @Override
    public Vehiculo guardar(Vehiculo vehiculo) {
        return VehiculoMapper.toDomain(
                jpaRepository.save(VehiculoMapper.toEntity(vehiculo))
        );
    }

    @Override
    public Optional<Vehiculo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(VehiculoMapper::toDomain);
    }

    @Override
    public boolean existePorPlaca(String placa) {
        return jpaRepository.existsByPlaca(placa);
    }

    @Override
    public List<Vehiculo> buscarTodos() {
        return jpaRepository.findAll().stream()
                .map(VehiculoMapper::toDomain)
                .toList();
    }
}
