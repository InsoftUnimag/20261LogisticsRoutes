package com.logistics.routes.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla `usuarios`.
 * Usada exclusivamente por la capa de seguridad (CustomUserDetailsService).
 * No tiene representación en el dominio — Auth es transversal.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Rol del usuario: DISPATCHER | DRIVER | FLEET_ADMIN | SYSTEM */
    @Column(nullable = false, length = 50)
    private String rol;

    /** Referencia al conductor asociado. Null si el usuario no es conductor. */
    @Column(name = "conductor_id")
    private UUID conductorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
