package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.usecase.AsignarVehiculoConductorUseCase;
import com.logistics.routes.application.usecase.ConsultarHistorialConductorUseCase;
import com.logistics.routes.application.usecase.DarDeBajaConductorUseCase;
import com.logistics.routes.application.usecase.DesvincularVehiculoConductorUseCase;
import com.logistics.routes.application.usecase.RegistrarConductorUseCase;
import com.logistics.routes.infrastructure.dto.request.AsignacionRequest;
import com.logistics.routes.infrastructure.dto.request.ConductorRequest;
import com.logistics.routes.infrastructure.dto.response.ConductorResponse;
import com.logistics.routes.infrastructure.dto.response.HistorialAsignacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conductores")
@PreAuthorize("hasRole('FLEET_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Conductores", description = "API de gestión de conductores y sus asignaciones")
public class ConductorController {

    private final RegistrarConductorUseCase registrarConductor;
    private final AsignarVehiculoConductorUseCase asignarVehiculo;
    private final DesvincularVehiculoConductorUseCase desvincularVehiculo;
    private final DarDeBajaConductorUseCase darDeBajaConductor;
    private final ConsultarHistorialConductorUseCase consultarHistorial;

    @Operation(summary = "Registrar un nuevo conductor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conductor registrado exitosamente"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado"),
            @ApiResponse(responseCode = "422", description = "Datos de petición inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse registrar(@Valid @RequestBody ConductorRequest request) {
        return ConductorResponse.from(registrarConductor.ejecutar(request.toCommand()));
    }

    @Operation(summary = "Asignar vehículo a conductor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignación exitosa"),
            @ApiResponse(responseCode = "404", description = "Conductor o vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor o vehículo no disponible")
    })
    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public ConductorResponse asignar(@Parameter(description = "ID del conductor") @PathVariable UUID id,
                                     @Valid @RequestBody AsignacionRequest request) {
        return ConductorResponse.from(asignarVehiculo.ejecutar(id, request.vehiculoId()));
    }

    @Operation(summary = "Desvincular vehículo de conductor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desvinculación exitosa"),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    })
    @DeleteMapping("/{id}/asignacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public void desvincular(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        desvincularVehiculo.ejecutar(id);
    }

    @Operation(summary = "Dar de baja un conductor")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conductor dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor tiene vehículo asignado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        darDeBajaConductor.ejecutar(id);
    }

    @Operation(summary = "Consultar historial de asignaciones")
    @ApiResponse(responseCode = "200", description = "Historial recuperado exitosamente")
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public List<HistorialAsignacionResponse> historial(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        return consultarHistorial.ejecutar(id).stream()
                .map(HistorialAsignacionResponse::from)
                .toList();
    }
}
