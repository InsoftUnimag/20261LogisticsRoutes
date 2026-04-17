package com.logistics.routes.domain.exception;

public class VehiculoEnTransitoException extends DominioException {

    public VehiculoEnTransitoException(String vehiculoId) {
        super("El vehículo " + vehiculoId + " se encuentra en tránsito y no puede ser modificado");
    }
}
