package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.application.event.RutaCerradaEvent;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
public class SqsIntegracionModulo3Adapter implements IntegracionModulo3Port {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegracionModulo3Adapter.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final String cierreRutaQueue;

    public SqsIntegracionModulo3Adapter(SqsTemplate sqsTemplate, ObjectMapper objectMapper,
                                        @Value("${app.sqs.cierre-ruta-queue}") String cierreRutaQueue) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.cierreRutaQueue = cierreRutaQueue;
    }

    @SuppressWarnings("null") // writeValueAsString nunca retorna null; lanza JsonProcessingException si falla
    @Override
    public void publishRutaCerrada(RutaCerradaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            sqsTemplate.send(cierreRutaQueue, payload);
            log.info("[M3-SQS] RUTA_CERRADA enviado a {} ruta_id={}", cierreRutaQueue, event.rutaId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando evento RUTA_CERRADA: " + event, e);
        }
    }
}
