package com.logistics.routes.domain.exception;

public class PlacaDuplicadaException extends DominioException {

    public PlacaDuplicadaException(String placa) {
        super("Ya existe un vehículo registrado con la placa: " + placa);
    }
}
