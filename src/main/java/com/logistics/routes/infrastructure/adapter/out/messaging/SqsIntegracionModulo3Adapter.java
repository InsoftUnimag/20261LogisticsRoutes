package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 *
 * <p>El payload se convierte a {@link JsonNode} usando el {@link ObjectMapper}
 * de Spring (con JavaTimeModule + snake_case) antes de enviarlo.
 * Esto hace que Spring Cloud AWS registre {@code JavaType=ObjectNode} en lugar
 * del nombre de nuestra clase interna, evitando que M3 falle al deserializar
 * por no tener {@code com.logistics.routes...RutaCerradaEvent} en su classpath.</p>
 */
@Component
@Profile("aws")
@RequiredArgsConstructor
public class SqsIntegracionModulo3Adapter implements IntegracionModulo3Port {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegracionModulo3Adapter.class);

    private final SqsTemplate sqsTemplate;
    /** ObjectMapper de Spring: incluye JavaTimeModule (Instant→ISO-8601) y snake_case. */
    private final ObjectMapper objectMapper;

    @Value("${app.sqs.cierre-ruta-queue}")
    private String cierreRutaQueue;

    @Override
    public void publishRutaCerrada(RutaCerradaEvent event) {
        JsonNode payload = objectMapper.valueToTree(event);
        sqsTemplate.send(cierreRutaQueue, payload);
        log.info("[M3-SQS] RUTA_CERRADA enviado a {} ruta_id={}", cierreRutaQueue, event.rutaId());
    }
}
