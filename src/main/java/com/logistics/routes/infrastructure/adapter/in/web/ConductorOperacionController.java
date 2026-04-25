package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.command.IniciarTransitoCommand;
import com.logistics.routes.application.usecase.CerrarRutaManualUseCase;
import com.logistics.routes.application.usecase.ConsultarRutaActivaUseCase;
import com.logistics.routes.application.usecase.IniciarTransitoUseCase;
import com.logistics.routes.application.usecase.RegistrarParadaUseCase;
import com.logistics.routes.application.usecase.SubirFotoParadaUseCase;
import com.logistics.routes.infrastructure.dto.request.CierreRutaRequest;
import com.logistics.routes.infrastructure.dto.request.RegistrarParadaRequest;
import com.logistics.routes.infrastructure.dto.response.RutaConductorResponse;
import com.logistics.routes.infrastructure.dto.response.SubirFotoResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private final SubirFotoParadaUseCase subirFotoParada;
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
    public ResponseEntity<RutaConductorResponse> rutaActiva(Authentication auth) {
        UUID conductorId = contexto.obtenerConductorId(auth);
        return consultarRutaActiva.ejecutar(conductorId)
                .map(view -> ResponseEntity.ok(RutaConductorResponse.from(view.ruta(), view.paradas())))
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

    @Operation(summary = "Subir foto de evidencia (POD) de una parada",
            description = "Carga el archivo de la foto al almacenamiento (local en dev/test, S3 en prod) y "
                    + "retorna la URL para que el conductor la incluya en el registro de la parada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto almacenada — URL retornada"),
            @ApiResponse(responseCode = "404", description = "Parada no encontrada"),
            @ApiResponse(responseCode = "422", description = "Archivo vacío o tipo no soportado")
    })
    @PostMapping(value = "/paradas/{paradaId}/foto", consumes = "multipart/form-data")
    public SubirFotoResponse subirFoto(
            @Parameter(description = "ID de la parada") @PathVariable UUID paradaId,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El archivo de foto no puede estar vacío");
        }
        String url = subirFotoParada.ejecutar(paradaId, archivo.getBytes(), archivo.getContentType());
        return new SubirFotoResponse(url);
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
    @PostMapping("/paradas/{paradaId}/registrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarResultado(
            @Parameter(description = "ID de la parada") @PathVariable UUID paradaId,
            @Valid @RequestBody RegistrarParadaRequest request) {
        registrarParada.ejecutar(request.toCommand(paradaId));
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
