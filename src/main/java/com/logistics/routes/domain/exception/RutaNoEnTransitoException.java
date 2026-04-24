package com.logistics.routes.domain.exception;

import java.util.UUID;

public class RutaNoEnTransitoException extends DominioException {

    public RutaNoEnTransitoException(UUID rutaId) {
        super("La ruta " + rutaId + " no se encuentra en estado EN_TRANSITO");
    }
}
