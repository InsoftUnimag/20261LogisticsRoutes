package com.logistics.routes.domain.exception;

public class ParadasPendientesException extends DominioException {

    public ParadasPendientesException(int cantidad) {
        super("Hay " + cantidad + " parada(s) en estado PENDIENTE sin gestionar. "
                + "Use confirmarConPendientes=true para marcarlas SIN_GESTION y forzar el cierre.");
    }
}
