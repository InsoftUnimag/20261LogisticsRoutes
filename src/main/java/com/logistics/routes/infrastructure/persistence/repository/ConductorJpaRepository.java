package com.logistics.routes.infrastructure.persistence.repository;

import com.logistics.routes.infrastructure.persistence.entity.ConductorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConductorJpaRepository extends JpaRepository<ConductorEntity, UUID> {
    boolean existsByEmail(String email);
}
