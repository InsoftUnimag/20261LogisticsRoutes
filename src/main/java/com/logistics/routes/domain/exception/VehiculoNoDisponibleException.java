package com.logistics.routes.domain.exception;

public class VehiculoNoDisponibleException extends DominioException {

    public VehiculoNoDisponibleException(String vehiculoId) {
        super("El vehículo " + vehiculoId + " no está disponible para ser asignado");
    }
}
