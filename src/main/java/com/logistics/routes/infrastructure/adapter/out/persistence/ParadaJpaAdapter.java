package com.logistics.routes.infrastructure.adapter.out.persistence;

import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.infrastructure.persistence.entity.ParadaEntity;
import com.logistics.routes.infrastructure.persistence.mapper.ParadaMapper;
import com.logistics.routes.infrastructure.persistence.repository.ParadaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParadaJpaAdapter implements ParadaRepositoryPort {

    private final ParadaJpaRepository jpaRepository;

    @Override
    public Parada guardar(Parada parada) {
        return ParadaMapper.toDomain(
                jpaRepository.save(ParadaMapper.toEntity(parada))
        );
    }

    @Override
    public List<Parada> guardarTodas(List<Parada> paradas) {
        List<ParadaEntity> entidades = paradas.stream()
                .map(ParadaMapper::toEntity)
                .toList();
        return jpaRepository.saveAll(entidades).stream()
                .map(ParadaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Parada> buscarPorRutaId(UUID rutaId) {
        return jpaRepository.findByRutaId(rutaId).stream()
                .map(ParadaMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Parada> buscarPorRutaYPaquete(UUID rutaId, UUID paqueteId) {
        return jpaRepository
                .findByRutaIdAndPaqueteId(rutaId, paqueteId)
                .map(ParadaMapper::toDomain);
    }
}
