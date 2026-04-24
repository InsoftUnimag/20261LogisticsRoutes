package com.logistics.routes.infrastructure.persistence.repository;

import com.logistics.routes.infrastructure.persistence.entity.ParadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParadaJpaRepository extends JpaRepository<ParadaEntity, UUID> {

    List<ParadaEntity> findByRutaId(UUID rutaId);

    Optional<ParadaEntity> findByRutaIdAndPaqueteId(UUID rutaId, UUID paqueteId);
}
