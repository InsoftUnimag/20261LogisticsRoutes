package com.logistics.routes.infrastructure.security;

import com.logistics.routes.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService que carga el usuario desde la tabla `usuarios`.
 * <p>
 * Spring Security lo invoca únicamente durante el proceso de login
 * (AuthenticationManager.authenticate). No se llama en requests posteriores —
 * eso lo maneja JwtAuthenticationFilter directamente desde el token.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioJpaRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> User.builder()
                        .username(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())))
                        .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
