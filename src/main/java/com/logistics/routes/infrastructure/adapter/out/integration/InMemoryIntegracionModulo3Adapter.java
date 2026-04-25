package com.logistics.routes.infrastructure.adapter.out.integration;

import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.event.RutaCerradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub en memoria de la integración con Módulo 3 (Liquidación).
 * Loguea el evento RUTA_CERRADA con el payload completo para trazabilidad.
 * Reemplazar por implementación SQS real bajo el perfil "aws" en F22.
 */
@Component
public class InMemoryIntegracionModulo3Adapter implements IntegracionModulo3Port {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIntegracionModulo3Adapter.class);

    @Override
    public void publishRutaCerrada(RutaCerradaEvent event) {
        log.info("[M3-EVENT] RUTA_CERRADA ruta_id={} tipo_cierre={} conductor_id={} modelo_contrato={} vehiculo_id={} paradas={}",
                event.rutaId(), event.tipoCierre(),
                event.conductor().conductorId(), event.conductor().modeloContrato(),
                event.vehiculo().vehiculoId(), event.paradas().size());
    }
}
