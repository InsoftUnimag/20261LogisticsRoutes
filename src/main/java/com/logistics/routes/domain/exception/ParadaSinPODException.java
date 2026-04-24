package com.logistics.routes.domain.exception;

import java.util.UUID;

public class ParadaSinPODException extends DominioException {

    public ParadaSinPODException(UUID paqueteId) {
        super("La parada del paquete " + paqueteId + " requiere foto de evidencia (POD) para ser marcada como exitosa");
    }
}
