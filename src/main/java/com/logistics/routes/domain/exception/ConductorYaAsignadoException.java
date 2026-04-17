package com.logistics.routes.domain.exception;

public class ConductorYaAsignadoException extends DominioException {

    public ConductorYaAsignadoException(String conductorId) {
        super("El conductor " + conductorId + " ya tiene un vehículo asignado");
    }
}
