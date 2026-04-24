package com.logistics.routes.domain.exception;

import java.util.UUID;

public class RutaNoEncontradaException extends DominioException {

    public RutaNoEncontradaException(UUID rutaId) {
        super("No se encontró ninguna ruta con id: " + rutaId);
    }
}
