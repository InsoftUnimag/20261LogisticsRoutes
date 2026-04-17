package com.logistics.routes.domain.exception;

public class ConductorNoDisponibleException extends DominioException {

    public ConductorNoDisponibleException(String conductorId) {
        super("El conductor " + conductorId + " no está activo y no puede recibir asignaciones");
    }
}
