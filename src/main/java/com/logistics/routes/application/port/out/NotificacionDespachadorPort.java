package com.logistics.routes.application.port.out;

import java.util.UUID;

public interface NotificacionDespachadorPort {
    void notificarAsignacion(UUID conductorId, UUID vehiculoId);
    void notificarDesvinculacion(UUID conductorId, UUID vehiculoId);
}
