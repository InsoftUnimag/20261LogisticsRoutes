package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.logistics.routes.application.event.RutaCerradaEvent;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación AWS SQS del puerto hacia Módulo 3 (Liquidación).
 * Publica el evento RUTA_CERRADA (SPEC-08 sección 4) en la cola configurada
 * {@code app.sqs.cierre-ruta-queue}. Solo activo bajo el perfil {@code aws}.
 */
@Component
@Profile("aws")
@RequiredArgsConstructor
public class SqsIntegracionModulo3Adapter implements IntegracionModulo3Port {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegracionModulo3Adapter.class);

    private final SqsTemplate sqsTemplate;

    @Value("${app.sqs.cierre-ruta-queue}")
    private String cierreRutaQueue;

    @Override
    public void publishRutaCerrada(RutaCerradaEvent event) {
        sqsTemplate.send(cierreRutaQueue, event);
        log.info("[M3-SQS] RUTA_CERRADA enviado a {} ruta_id={}", cierreRutaQueue, event.rutaId());
    }
}
