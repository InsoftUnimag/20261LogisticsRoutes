package com.logistics.routes.infrastructure.adapter.out.integration;

import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Stub en memoria de la integración con Módulo 1 (SGP).
 * Loguea cada evento con el payload plano para trazabilidad.
 * Reemplazar por implementación SQS real bajo el perfil "aws" en F22.
 */
@Component
public class InMemoryIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIntegracionModulo1Adapter.class);

    @Override
    public void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora) {
        log.info("[M1-EVENT] PAQUETE_EXCLUIDO_DESPACHO paquete_id={} ruta_id={} motivo='{}' timestamp={}",
                paqueteId, rutaId, motivo, fechaHora);
    }

    @Override
    public void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        log.info("[M1-EVENT] PAQUETE_EN_TRANSITO paquete_id={} ruta_id={} timestamp={}",
                paqueteId, rutaId, fechaHora);
    }
}
