package com.logistics.routes.infrastructure.adapter.out.notification;

import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.domain.enums.TipoVehiculo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub en memoria de notificaciones al despachador.
 * Reemplazar por implementación WebSocket cuando esté disponible en el sprint correspondiente.
 */
@Component
public class InMemoryNotificacionAdapter implements NotificacionDespachadorPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryNotificacionAdapter.class);

    @Override
    public void notificarAsignacion(UUID conductorId, UUID vehiculoId) {
        log.info("[NOTIFICACION] Conductor {} asignado al vehículo {}", conductorId, vehiculoId);
    }

    @Override
    public void notificarDesvinculacion(UUID conductorId, UUID vehiculoId) {
        log.info("[NOTIFICACION] Conductor {} desvinculado del vehículo {}", conductorId, vehiculoId);
    }

    @Override
    public void notificarRutaListaParaDespacho(UUID rutaId, String zona, double pesoKg,
                                               TipoVehiculo tipoVehiculo, String motivo) {
        log.info("[NOTIFICACION] Ruta {} en zona {} lista para despacho — peso={}kg, vehículo={}, motivo={}",
                rutaId, zona, pesoKg, tipoVehiculo, motivo);
    }

    @Override
    public void notificarAlertaPrioritaria(String mensaje) {
        log.warn("[ALERTA-PRIORITARIA] {}", mensaje);
    }
}
