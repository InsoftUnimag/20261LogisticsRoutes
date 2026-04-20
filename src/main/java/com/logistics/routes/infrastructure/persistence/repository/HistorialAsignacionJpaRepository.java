package com.logistics.routes.infrastructure.persistence.repository;

import com.logistics.routes.infrastructure.persistence.entity.HistorialAsignacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialAsignacionJpaRepository extends JpaRepository<HistorialAsignacionEntity, UUID> {
    Optional<HistorialAsignacionEntity> findByConductorIdAndFechaHoraFinIsNull(UUID conductorId);
    List<HistorialAsignacionEntity> findByConductorId(UUID conductorId);
}
