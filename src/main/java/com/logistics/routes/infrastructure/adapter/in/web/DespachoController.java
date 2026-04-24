package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.usecase.ConfirmarDespachoUseCase;
import com.logistics.routes.application.usecase.ExcluirPaqueteRutaUseCase;
import com.logistics.routes.application.usecase.ListarRutasParaDespachoUseCase;
import com.logistics.routes.application.usecase.ObtenerDetalleRutaUseCase;
import com.logistics.routes.application.usecase.ObtenerDetalleRutaUseCase.Detalle;
import com.logistics.routes.infrastructure.dto.request.ConfirmarDespachoRequest;
import com.logistics.routes.infrastructure.dto.response.RutaDetalleResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/despacho")
@RequiredArgsConstructor
@Tag(name = "Despacho", description = "API de despacho de rutas por el Despachador Logístico")
public class DespachoController {

    private final ListarRutasParaDespachoUseCase listarRutas;
    private final ConfirmarDespachoUseCase confirmarDespacho;
    private final ExcluirPaqueteRutaUseCase excluirPaquete;
    private final ObtenerDetalleRutaUseCase obtenerDetalle;

    @Operation(summary = "Listar rutas listas para despacho",
            description = "Retorna todas las rutas en estado LISTA_PARA_DESPACHO con su detalle y paradas ordenadas. Requiere rol DISPATCHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido exitosamente")
    })
    @GetMapping("/rutas")
    @PreAuthorize("hasRole('DISPATCHER')")
    public List<RutaDetalleResponse> listar() {
        return listarRutas.ejecutar().stream()
                .map(ruta -> obtenerDetalle.ejecutar(ruta.getId()))
                .map(d -> RutaDetalleResponse.from(d.ruta(), d.paradas()))
                .toList();
    }

    @Operation(summary = "Confirmar despacho de una ruta",
            description = "Asigna conductor y vehículo, optimiza el orden de paradas y transiciona la ruta a CONFIRMADA. Requiere rol DISPATCHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta confirmada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ruta, conductor o vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor no activo o vehículo no disponible"),
            @ApiResponse(responseCode = "422", description = "Datos de la solicitud inválidos")
    })
    @PostMapping("/rutas/{id}/confirmar")
    @PreAuthorize("hasRole('DISPATCHER')")
    public RutaDetalleResponse confirmar(
            @Parameter(description = "ID de la ruta a confirmar") @PathVariable UUID id,
            @Valid @RequestBody ConfirmarDespachoRequest request) {
        confirmarDespacho.ejecutar(id, request.toCommand());
        Detalle d = obtenerDetalle.ejecutar(id);
        return RutaDetalleResponse.from(d.ruta(), d.paradas());
    }

    @Operation(summary = "Excluir paquete de una ruta antes de confirmar",
            description = "Marca la parada como EXCLUIDA_DESPACHO y emite el evento PAQUETE_EXCLUIDO_DESPACHO al Módulo 1. Requiere rol DISPATCHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paquete excluido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Parada no encontrada para el paquete indicado")
    })
    @DeleteMapping("/rutas/{id}/paquetes/{paqueteId}")
    @PreAuthorize("hasRole('DISPATCHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @Parameter(description = "ID de la ruta") @PathVariable UUID id,
            @Parameter(description = "ID del paquete a excluir") @PathVariable UUID paqueteId,
            @Parameter(description = "Motivo de la exclusión") @RequestParam String motivo) {
        excluirPaquete.ejecutar(id, paqueteId, motivo);
    }

    @Operation(summary = "Forzar cierre de una ruta",
            description = "Cierra manualmente una ruta sin esperar al conductor. Stub hasta la implementación de F18.")
    @ApiResponses({
            @ApiResponse(responseCode = "501", description = "Funcionalidad no implementada aún — disponible en F18")
    })
    @PostMapping("/rutas/{id}/forzar-cierre")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<Void> forzarCierre(
            @Parameter(description = "ID de la ruta") @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
