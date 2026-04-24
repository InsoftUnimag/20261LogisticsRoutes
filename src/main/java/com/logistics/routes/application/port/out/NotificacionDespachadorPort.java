package com.logistics.routes.application.port.out;

import com.logistics.routes.domain.enums.TipoVehiculo;

import java.util.UUID;

public interface NotificacionDespachadorPort {
    void notificarAsignacion(UUID conductorId, UUID vehiculoId);

    void notificarDesvinculacion(UUID conductorId, UUID vehiculoId);

    void notificarRutaListaParaDespacho(UUID rutaId, String zona, double pesoKg,
            TipoVehiculo tipoVehiculo, String motivo);

    void notificarAlertaPrioritaria(String mensaje);
}
