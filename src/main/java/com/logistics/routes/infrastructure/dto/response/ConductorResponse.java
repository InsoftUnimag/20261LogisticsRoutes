package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.model.Conductor;

import java.util.UUID;

public record ConductorResponse(
        UUID id,
        String nombre,
        String email,
        ModeloContrato modeloContrato,
        EstadoConductor estado,
        UUID vehiculoAsignadoId
) {
    public static ConductorResponse from(Conductor c) {
        return new ConductorResponse(
                c.getId(),
                c.getNombre(),
                c.getEmail(),
                c.getModeloContrato(),
                c.getEstado(),
                c.getVehiculoAsignadoId()
        );
    }
}
