package com.logistics.routes.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro HTTP que intercepta cada request, extrae el Bearer token del header
 * Authorization, lo valida con JwtService y popula el SecurityContext.
 * <p>
 * No hace llamada a BD en cada request — lee los claims directamente del token
 * para mantener la arquitectura stateless sin overhead.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null && jwtService.esValido(token)) {
            String email = jwtService.extraerEmail(token);
            String rol   = jwtService.extraerRol(token);

            // Spring Security espera "ROLE_" como prefijo cuando se usa hasRole()
            var authority      = new SimpleGrantedAuthority("ROLE_" + rol);
            var authentication = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token del header "Authorization: Bearer <token>".
     * Retorna null si el header no existe o no tiene el formato correcto.
     */
    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
