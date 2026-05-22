package com.logistics.routes.domain.exception;

import java.util.UUID;

public class ConductorNoAsignadoAlVehiculoException extends DominioException {

    public ConductorNoAsignadoAlVehiculoException(UUID conductorIntentado,
                                                  UUID vehiculoId,
                                                  UUID conductorAsignado) {
        super(String.format(
                "El conductor %s no está emparejado con el vehículo %s (emparejado actualmente: %s)",
                conductorIntentado,
                vehiculoId,
                conductorAsignado != null ? conductorAsignado : "ninguno"
        ));
    }
}
