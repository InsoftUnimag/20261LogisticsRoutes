package com.logistics.routes.infrastructure.adapter.in.web;

import com.logistics.routes.application.usecase.ActualizarVehiculoUseCase;
import com.logistics.routes.application.usecase.ConsultarDisponibilidadFlotaUseCase;
import com.logistics.routes.application.usecase.DarDeBajaVehiculoUseCase;
import com.logistics.routes.application.usecase.RegistrarVehiculoUseCase;
import com.logistics.routes.infrastructure.dto.request.VehiculoRequest;
import com.logistics.routes.infrastructure.dto.response.FlotaDisponibilidadResponse;
import com.logistics.routes.infrastructure.dto.response.VehiculoResponse;
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
@RequestMapping("/api/vehiculos")
@PreAuthorize("hasRole('FLEET_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Vehículos", description = "API de gestión de flota de vehículos")
public class VehiculoController {

    private final RegistrarVehiculoUseCase registrarVehiculo;
    private final ActualizarVehiculoUseCase actualizarVehiculo;
    private final DarDeBajaVehiculoUseCase darDeBajaVehiculo;
    private final ConsultarDisponibilidadFlotaUseCase consultarDisponibilidad;

    @Operation(summary = "Registrar un nuevo vehículo", description = "Registra un vehículo en la flota. Requiere rol FLEET_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehículo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de petición inválidos"),
            @ApiResponse(responseCode = "409", description = "Placa duplicada")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse registrar(@Valid @RequestBody VehiculoRequest request) {
        return VehiculoResponse.from(
                registrarVehiculo.ejecutar(request.toRegistrarCommand())
        );
    }

    @Operation(summary = "Listar todos los vehículos", description = "Obtiene la lista completa de vehículos. Requiere rol FLEET_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente")
    @GetMapping
    public List<VehiculoResponse> listar() {
        return consultarDisponibilidad.ejecutar().stream()
                .map(VehiculoResponse::from)
                .toList();
    }

    @Operation(summary = "Actualizar vehículo", description = "Actualiza los datos de un vehículo existente. Requiere rol FLEET_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Vehículo en tránsito, no se puede actualizar o placa duplicada")
    })
    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@Parameter(description = "ID del vehículo") @PathVariable UUID id,
                                       @Valid @RequestBody VehiculoRequest request) {
        return VehiculoResponse.from(
                actualizarVehiculo.ejecutar(id, request.toActualizarCommand())
        );
    }

    @Operation(summary = "Dar de baja un vehículo", description = "Marca un vehículo como INACTIVO. Requiere rol FLEET_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehículo dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Vehículo en tránsito, no se puede dar de baja")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Parameter(description = "ID del vehículo") @PathVariable UUID id) {
        darDeBajaVehiculo.ejecutar(id);
    }

    @Operation(summary = "Consultar disponibilidad de la flota", description = "Obtiene el estado de disponibilidad de todos los vehículos y si están listos para planificación. Requiere rol FLEET_ADMIN o DISPATCHER.")
    @ApiResponse(responseCode = "200", description = "Disponibilidad recuperada exitosamente")
    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('FLEET_ADMIN', 'DISPATCHER')")
    public List<FlotaDisponibilidadResponse> disponibilidad() {
        return consultarDisponibilidad.ejecutar().stream()
                .map(FlotaDisponibilidadResponse::from)
                .toList();
    }
}
