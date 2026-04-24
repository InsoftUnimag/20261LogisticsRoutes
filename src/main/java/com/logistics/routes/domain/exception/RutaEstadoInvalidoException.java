package com.logistics.routes.domain.exception;

import com.logistics.routes.domain.enums.EstadoRuta;

import java.util.UUID;

public class RutaEstadoInvalidoException extends DominioException {

    public RutaEstadoInvalidoException(UUID rutaId, EstadoRuta esperado, EstadoRuta actual) {
        super("La ruta " + rutaId + " requiere estado " + esperado + " pero tiene estado " + actual);
    }
}
