package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.infrastructure.dto.request.LoginRequest;
import com.logistics.routes.infrastructure.dto.response.LoginResponse;
import com.logistics.routes.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 * <p>
 * Ruta pública — no requiere token previo.
 * Devuelve un JWT firmado que el cliente debe incluir en requests posteriores
 * como: Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoint de login para obtener JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(
            summary = "Login de usuario",
            description = "Autentica con email y password. Retorna JWT con rol y tiempo de expiración."
    )
    @ApiResponse(responseCode = "200", description = "Login exitoso — retorna token JWT")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @ApiResponse(responseCode = "422", description = "Request con campos inválidos")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // Spring Security valida email + password contra la BD (BCrypt compare)
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String email = auth.getName();
        String rol = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");

        String token = jwtService.generarToken(email, rol);

        return ResponseEntity.ok(new LoginResponse(
                token,
                rol,
                jwtService.calcularExpiracion()
        ));
    }
}
