package com.logistics.routes.infrastructure.adapter.out.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.application.event.RutaCerradaEvent;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub en memoria de la integración con Módulo 3 (Liquidación).
 * Loguea el evento RUTA_CERRADA con el payload JSON completo según SPEC-08 sección 4.
 * Reemplazar por implementación SQS real bajo el perfil "aws" en F22.
 */
@Component
@RequiredArgsConstructor
public class InMemoryIntegracionModulo3Adapter implements IntegracionModulo3Port {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIntegracionModulo3Adapter.class);

    private final ObjectMapper objectMapper;

    @Override
    public void publishRutaCerrada(RutaCerradaEvent event) {
        try {
            log.info("[M3-EVENT] {}", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando evento RUTA_CERRADA: " + event, e);
        }
    }
}
