package com.logistics.routes.domain.exception;

import java.util.UUID;

public class ConductorNoAsignadoARutaException extends DominioException {

    public ConductorNoAsignadoARutaException(UUID conductorId, UUID rutaId) {
        super("El conductor " + conductorId + " no está asignado a la ruta " + rutaId);
    }
}
