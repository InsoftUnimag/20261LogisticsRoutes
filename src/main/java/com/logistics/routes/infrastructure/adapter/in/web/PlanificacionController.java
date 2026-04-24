package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.usecase.DespacharManualUseCase;
import com.logistics.routes.application.usecase.SolicitarRutaUseCase;
import com.logistics.routes.infrastructure.dto.request.DespacharManualRequest;
import com.logistics.routes.infrastructure.dto.request.SolicitarRutaRequest;
import com.logistics.routes.infrastructure.dto.response.SolicitarRutaResponse;
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
@RequestMapping("/api/planificacion")
@RequiredArgsConstructor
@Tag(name = "Planificación", description = "API de planificación y despacho de rutas logísticas")
public class PlanificacionController {

        private final SolicitarRutaUseCase solicitarRuta;
        private final DespacharManualUseCase despacharManual;

        @Operation(summary = "Solicitar asignación de paquete a ruta", description = "Recibe un paquete del SGP y lo consolida en una ruta activa de la zona geográfica. "
                        +
                        "Crea una nueva ruta si no existe ninguna en CREADA para la zona. Requiere rol SYSTEM.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Paquete asignado a ruta exitosamente"),
                        @ApiResponse(responseCode = "422", description = "Fecha límite de entrega ya vencida"),
                        @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos")
        })
        @PostMapping("/solicitar-ruta")
        @PreAuthorize("hasRole('SYSTEM')")
        @ResponseStatus(HttpStatus.OK)
        public SolicitarRutaResponse solicitarRuta(@Valid @RequestBody SolicitarRutaRequest request) {
                UUID rutaId = solicitarRuta.ejecutar(request.toCommand());
                return new SolicitarRutaResponse(rutaId);
        }

        @Operation(summary = "Confirmar ruta para despacho", description = "El despachador asigna conductor y vehículo a una ruta en LISTA_PARA_DESPACHO, "
                        +
                        "transicionándola a CONFIRMADA. Requiere rol DISPATCHER.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Ruta confirmada exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Ruta no encontrada"),
                        @ApiResponse(responseCode = "400", description = "Datos de la solicitud inválidos o estado de ruta incorrecto")
        })
        @PostMapping("/rutas/{id}/despacho-manual")
        @PreAuthorize("hasRole('DISPATCHER')")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void despachoManual(
                        @Parameter(description = "ID de la ruta a confirmar") @PathVariable UUID id,
                        @Valid @RequestBody DespacharManualRequest request) {
                despacharManual.ejecutar(id, request.conductorId(), request.vehiculoId());
        }
}
