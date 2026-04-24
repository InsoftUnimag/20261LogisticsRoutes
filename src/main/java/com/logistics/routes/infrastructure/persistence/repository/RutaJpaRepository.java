package com.logistics.routes.infrastructure.persistence.repository;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.infrastructure.persistence.entity.RutaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RutaJpaRepository extends JpaRepository<RutaEntity, UUID> {

    Optional<RutaEntity> findByZonaAndEstado(String zona, EstadoRuta estado);

    List<RutaEntity> findByEstado(EstadoRuta estado);

    List<RutaEntity> findByEstadoAndFechaLimiteDespachoLessThanEqual(EstadoRuta estado, Instant limite);

    List<RutaEntity> findByEstadoAndFechaHoraInicioLessThanEqual(EstadoRuta estado, Instant limite);

    Optional<RutaEntity> findFirstByConductorIdAndEstadoIn(UUID conductorId, Collection<EstadoRuta> estados);
}
