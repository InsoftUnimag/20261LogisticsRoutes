package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.port.in.ActualizarVehiculoPort;
import com.logistics.routes.application.port.in.ConsultarDisponibilidadFlotaPort;
import com.logistics.routes.application.port.in.DarDeBajaVehiculoPort;
import com.logistics.routes.application.port.in.RegistrarVehiculoPort;
import com.logistics.routes.infrastructure.dto.request.VehiculoRequest;
import com.logistics.routes.infrastructure.dto.response.FlotaDisponibilidadResponse;
import com.logistics.routes.infrastructure.dto.response.VehiculoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehiculos")
@PreAuthorize("hasRole('FLEET_ADMIN')")
@RequiredArgsConstructor
public class VehiculoController {

    private final RegistrarVehiculoPort registrarVehiculo;
    private final ActualizarVehiculoPort actualizarVehiculo;
    private final DarDeBajaVehiculoPort darDeBajaVehiculo;
    private final ConsultarDisponibilidadFlotaPort consultarDisponibilidad;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse registrar(@Valid @RequestBody VehiculoRequest request) {
        return VehiculoResponse.from(
                registrarVehiculo.ejecutar(request.toRegistrarCommand())
        );
    }

    @GetMapping
    public List<VehiculoResponse> listar() {
        return consultarDisponibilidad.ejecutar().stream()
                .map(VehiculoResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable UUID id,
                                       @Valid @RequestBody VehiculoRequest request) {
        return VehiculoResponse.from(
                actualizarVehiculo.ejecutar(id, request.toActualizarCommand())
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable UUID id) {
        darDeBajaVehiculo.ejecutar(id);
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public List<FlotaDisponibilidadResponse> disponibilidad() {
        return consultarDisponibilidad.ejecutar().stream()
                .map(FlotaDisponibilidadResponse::from)
                .toList();
    }
}
