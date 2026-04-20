package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.port.in.AsignarVehiculoConductorPort;
import com.logistics.routes.application.port.in.ConsultarHistorialConductorPort;
import com.logistics.routes.application.port.in.DarDeBajaConductorPort;
import com.logistics.routes.application.port.in.DesvincularVehiculoConductorPort;
import com.logistics.routes.application.port.in.RegistrarConductorPort;
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

    private final RegistrarConductorPort registrarConductor;
    private final AsignarVehiculoConductorPort asignarVehiculo;
    private final DesvincularVehiculoConductorPort desvincularVehiculo;
    private final DarDeBajaConductorPort darDeBajaConductor;
    private final ConsultarHistorialConductorPort consultarHistorial;

    @Operation(summary = "Registrar un nuevo conductor", description = "Registra un conductor en la plataforma. Requiere rol FLEET_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conductor registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de petición inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse registrar(@Valid @RequestBody ConductorRequest request) {
        return ConductorResponse.from(registrarConductor.ejecutar(request.toCommand()));
    }

    @Operation(summary = "Asignar vehículo a conductor", description = "Asigna un vehículo a un conductor, creando un nuevo historial de asignación. Requiere rol FLEET_ADMIN o DISPATCHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignación exitosa"),
            @ApiResponse(responseCode = "404", description = "Conductor o vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor o vehículo no disponible para asignación")
    })
    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public ConductorResponse asignar(@Parameter(description = "ID del conductor") @PathVariable UUID id,
                                     @Valid @RequestBody AsignacionRequest request) {
        return ConductorResponse.from(asignarVehiculo.ejecutar(id, request.vehiculoId()));
    }

    @Operation(summary = "Desvincular vehículo de conductor", description = "Desvincula al conductor de su vehículo actual y cierra el historial de asignación. Requiere rol FLEET_ADMIN o DISPATCHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desvinculación exitosa"),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor no tiene vehículo asignado o está en ruta")
    })
    @DeleteMapping("/{id}/asignacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public void desvincular(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        desvincularVehiculo.ejecutar(id);
    }

    @Operation(summary = "Dar de baja un conductor", description = "Marca un conductor como INACTIVO. Requiere rol FLEET_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conductor dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conductor está en ruta o tiene vehículo asignado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        darDeBajaConductor.ejecutar(id);
    }

    @Operation(summary = "Consultar historial de asignaciones", description = "Obtiene el historial de asignaciones de vehículos de un conductor. Requiere rol FLEET_ADMIN o DISPATCHER.")
    @ApiResponse(responseCode = "200", description = "Historial recuperado exitosamente")
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public List<HistorialAsignacionResponse> historial(@Parameter(description = "ID del conductor") @PathVariable UUID id) {
        return consultarHistorial.ejecutar(id).stream()
                .map(HistorialAsignacionResponse::from)
                .toList();
    }
}
