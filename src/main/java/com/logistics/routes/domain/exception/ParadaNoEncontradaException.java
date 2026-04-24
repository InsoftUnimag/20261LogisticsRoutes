package com.logistics.routes.domain.exception;

import java.util.UUID;

public class ParadaNoEncontradaException extends DominioException {

    public ParadaNoEncontradaException(UUID paqueteId) {
        super("No se encontró ninguna parada para el paquete: " + paqueteId);
    }
}
