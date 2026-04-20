package com.logistics.routes.domain.exception;

import java.util.UUID;

public class VehiculoNoEncontradoException extends DominioException {

    public VehiculoNoEncontradoException(UUID id) {
        super("No se encontró ningún vehículo con id: " + id);
    }
}
