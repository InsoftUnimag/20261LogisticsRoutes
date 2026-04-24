package com.logistics.routes.domain.exception;

import java.time.Instant;

public class FechaLimiteVencidaException extends DominioException {

    public FechaLimiteVencidaException(Instant fechaLimite) {
        super("La fecha límite ya venció: " + fechaLimite);
    }
}
