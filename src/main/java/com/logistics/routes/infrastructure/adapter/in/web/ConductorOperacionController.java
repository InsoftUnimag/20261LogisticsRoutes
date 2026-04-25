package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.command.IniciarTransitoCommand;
import com.logistics.routes.application.usecase.CerrarRutaManualUseCase;
import com.logistics.routes.application.usecase.ConsultarRutaActivaUseCase;
import com.logistics.routes.application.usecase.IniciarTransitoUseCase;
import com.logistics.routes.application.usecase.RegistrarParadaUseCase;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.infrastructure.dto.request.CierreRutaRequest;
import com.logistics.routes.infrastructure.dto.request.RegistrarParadaRequest;
import com.logistics.routes.infrastructure.dto.response.RutaActivaResponse;
import com.logistics.routes.infrastructure.security.ConductorContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final ConsultarRutaActivaUseCase consultarRutaActiva;
    private final IniciarTransitoUseCase iniciarTransito;
    private final RegistrarParadaUseCase registrarParada;
    private final CerrarRutaManualUseCase cerrarRuta;
    private final ConductorContextService contexto;

    @Operation(summary = "Consultar ruta activa del conductor",
            description = "Retorna la ruta del conductor en estado CONFIRMADA o EN_TRANSITO con sus paradas "
                    + "ordenadas. Si el conductor no tiene ruta asignada retorna 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta activa retornada"),
            @ApiResponse(responseCode = "204", description = "El conductor no tiene ruta activa")
    })
    @GetMapping("/ruta-activa")
    public ResponseEntity<RutaActivaResponse> rutaActiva(Authentication auth) {
        UUID conductorId = contexto.obtenerConductorId(auth);
        return consultarRutaActiva.ejecutar(conductorId)
                .map(view -> ResponseEntity.ok(RutaActivaResponse.from(view.ruta(), view.paradas())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Iniciar tránsito de una ruta",
            description = "Transiciona la ruta de CONFIRMADA a EN_TRANSITO y publica PAQUETE_EN_TRANSITO "
                    + "a Módulo 1 por cada parada pendiente. Solo el conductor asignado puede iniciarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tránsito iniciado"),
            @ApiResponse(responseCode = "403", description = "El conductor no es el asignado a la ruta"),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada"),
            @ApiResponse(responseCode = "409", description = "La ruta no está en estado CONFIRMADA")
    })
    @PostMapping("/rutas/{id}/iniciar-transito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarTransito(
            @Parameter(description = "ID de la ruta") @PathVariable UUID id,
            Authentication auth) {
        UUID conductorId = contexto.obtenerConductorId(auth);
        iniciarTransito.ejecutar(new IniciarTransitoCommand(id, conductorId));
    }

    @Operation(summary = "Registrar resultado de una parada",
            description = "Registra el resultado de la parada (EXITOSA, FALLIDA o NOVEDAD). "
                    + "Para EXITOSA, fotoUrl es obligatoria (POD). Publica el evento correspondiente a M1. "
                    + "El backend respeta fechaAccion del request (soporte offline).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resultado registrado"),
            @ApiResponse(responseCode = "404", description = "Parada o ruta no encontrada"),
            @ApiResponse(responseCode = "409", description = "La ruta no está EN_TRANSITO o la parada no está PENDIENTE"),
            @ApiResponse(responseCode = "422", description = "POD ausente para resultado EXITOSA o cuerpo inválido")
    })
    @PostMapping("/rutas/{rutaId}/paradas/{paradaId}/resultado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarResultado(
            @Parameter(description = "ID de la ruta") @PathVariable UUID rutaId,
            @Parameter(description = "ID de la parada") @PathVariable UUID paradaId,
            @Valid @RequestBody RegistrarParadaRequest request) {
        Parada actualizada = registrarParada.ejecutar(request.toCommand(paradaId));
        if (!actualizada.getRutaId().equals(rutaId)) {
            throw new IllegalArgumentException(
                    "La parada " + paradaId + " no pertenece a la ruta " + rutaId);
        }
    }

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
