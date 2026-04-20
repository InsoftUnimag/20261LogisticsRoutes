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
public class ConductorController {

    private final RegistrarConductorPort registrarConductor;
    private final AsignarVehiculoConductorPort asignarVehiculo;
    private final DesvincularVehiculoConductorPort desvincularVehiculo;
    private final DarDeBajaConductorPort darDeBajaConductor;
    private final ConsultarHistorialConductorPort consultarHistorial;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse registrar(@Valid @RequestBody ConductorRequest request) {
        return ConductorResponse.from(registrarConductor.ejecutar(request.toCommand()));
    }

    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public ConductorResponse asignar(@PathVariable UUID id,
                                     @Valid @RequestBody AsignacionRequest request) {
        return ConductorResponse.from(asignarVehiculo.ejecutar(id, request.vehiculoId()));
    }

    @DeleteMapping("/{id}/asignacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public void desvincular(@PathVariable UUID id) {
        desvincularVehiculo.ejecutar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable UUID id) {
        darDeBajaConductor.ejecutar(id);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public List<HistorialAsignacionResponse> historial(@PathVariable UUID id) {
        return consultarHistorial.ejecutar(id).stream()
                .map(HistorialAsignacionResponse::from)
                .toList();
    }
}
