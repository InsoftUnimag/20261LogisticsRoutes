package com.logistics.routes.infrastructure.persistence.repository;

import com.logistics.routes.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la tabla `usuarios`.
 * Usado únicamente por CustomUserDetailsService durante la autenticación.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);
}
