package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.usecase.CerrarRutaManualUseCase;
import com.logistics.routes.infrastructure.dto.request.CierreRutaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conductor")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
@Tag(name = "Conductor — Operación de campo", description = "Operaciones de campo ejecutadas por el conductor en ruta")
public class ConductorOperacionController {

    private final CerrarRutaManualUseCase cerrarRuta;

    @Operation(summary = "Cerrar ruta manualmente",
            description = "El conductor cierra su ruta activa. Si quedan paradas PENDIENTE y "
                    + "confirmarConPendientes=false, se rechaza con 409. Con confirmarConPendientes=true "
                    + "las paradas pendientes se marcan SIN_GESTION y la ruta queda CERRADA_MANUAL. "
                    + "Publica PARADAS_SIN_GESTIONAR a M1 (si aplica) y RUTA_CERRADA a M3.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ruta cerrada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ruta no está EN_TRANSITO o hay paradas pendientes sin confirmar"),
            @ApiResponse(responseCode = "422", description = "Cuerpo de la solicitud inválido")
    })
    @PostMapping("/rutas/{id}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarRuta(
            @Parameter(description = "ID de la ruta a cerrar") @PathVariable UUID id,
            @Valid @RequestBody CierreRutaRequest request) {
        cerrarRuta.ejecutar(id, request.confirmarConPendientes());
    }
}
