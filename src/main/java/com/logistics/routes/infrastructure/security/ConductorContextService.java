package com.logistics.routes.infrastructure.security;

import com.logistics.routes.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Resuelve el {@code conductorId} del usuario autenticado.
 * El JWT solo contiene el email; este servicio hace el mapeo a la entidad
 * {@code Conductor} via la tabla {@code usuarios.conductor_id}.
 */
@Service
@RequiredArgsConstructor
public class ConductorContextService {

    private final UsuarioJpaRepository usuarioRepository;

    public UUID obtenerConductorId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay autenticación activa");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .map(usuario -> {
                    if (usuario.getConductorId() == null) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "El usuario " + auth.getName() + " no tiene conductor asociado");
                    }
                    return usuario.getConductorId();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado: " + auth.getName()));
    }
}
