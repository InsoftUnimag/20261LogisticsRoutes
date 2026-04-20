package com.logistics.routes.domain.exception;

import java.util.UUID;

public class ConductorNoEncontradoException extends DominioException {

    public ConductorNoEncontradoException(UUID id) {
        super("No se encontró ningún conductor con id: " + id);
    }
}
