package com.logistics.routes.infrastructure.adapter.out.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.application.event.NovedadGraveEvent;
import com.logistics.routes.application.event.PaqueteEnTransitoEvent;
import com.logistics.routes.application.event.PaqueteEntregadoEvent;
import com.logistics.routes.application.event.PaqueteExcluidoDespachoEvent;
import com.logistics.routes.application.event.ParadaFallidaEvent;
import com.logistics.routes.application.event.ParadasSinGestionarEvent;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.domain.enums.TipoCierre;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stub en memoria de la integración con Módulo 1 (SGP).
 * Loguea cada evento con el payload JSON completo según SPEC-08 sección 3.
 * Reemplazar por implementación SQS real bajo el perfil "aws" en F22.
 */
@Component
@RequiredArgsConstructor
public class InMemoryIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIntegracionModulo1Adapter.class);

    private final ObjectMapper objectMapper;

    @Override
    public void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora) {
        publicar(PaqueteExcluidoDespachoEvent.of(paqueteId, rutaId, fechaHora));
    }

    @Override
    public void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        publicar(PaqueteEnTransitoEvent.of(paqueteId, rutaId, fechaHora));
    }

    @Override
    public void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fechaEntrega,
                                        String urlFoto, String urlFirma) {
        publicar(PaqueteEntregadoEvent.of(paqueteId, rutaId, fechaEntrega, urlFoto, urlFirma));
    }

    @Override
    public void publishParadaFallida(UUID paqueteId, UUID rutaId, String motivo, Instant fechaAccion) {
        publicar(ParadaFallidaEvent.of(paqueteId, rutaId, motivo, fechaAccion));
    }

    @Override
    public void publishNovedadGrave(UUID paqueteId, UUID rutaId, String tipoNovedad, Instant fechaAccion) {
        publicar(NovedadGraveEvent.of(paqueteId, rutaId, tipoNovedad, fechaAccion));
    }

    @Override
    public void publishParadasSinGestionar(UUID rutaId, TipoCierre tipoCierre, List<UUID> paqueteIds) {
        publicar(ParadasSinGestionarEvent.of(rutaId, tipoCierre, paqueteIds, Instant.now()));
    }

    private void publicar(Object evento) {
        try {
            log.info("[M1-EVENT] {}", objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando evento M1: " + evento, e);
        }
    }
}
