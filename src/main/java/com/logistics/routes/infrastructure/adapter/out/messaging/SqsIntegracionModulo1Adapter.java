package com.logistics.routes.infrastructure.adapter.out.messaging;

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
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Profile("aws")
public class SqsIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegracionModulo1Adapter.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final String eventosPaqueteQueue;

    public SqsIntegracionModulo1Adapter(SqsTemplate sqsTemplate, ObjectMapper objectMapper,
                                        @Value("${app.sqs.eventos-paquete-queue}") String eventosPaqueteQueue) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.eventosPaqueteQueue = eventosPaqueteQueue;
    }

    @Override
    public void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora) {
        publicar(PaqueteExcluidoDespachoEvent.of(paqueteId, rutaId, fechaHora), "PAQUETE_EXCLUIDO_DESPACHO");
    }

    @Override
    public void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        publicar(PaqueteEnTransitoEvent.of(paqueteId, rutaId, fechaHora), "PAQUETE_EN_TRANSITO");
    }

    @Override
    public void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fechaEntrega,
                                        String urlFoto, String urlFirma) {
        publicar(PaqueteEntregadoEvent.of(paqueteId, rutaId, fechaEntrega, urlFoto, urlFirma),
                "PAQUETE_ENTREGADO");
    }

    @Override
    public void publishParadaFallida(UUID paqueteId, UUID rutaId, String motivo, Instant fechaAccion) {
        publicar(ParadaFallidaEvent.of(paqueteId, rutaId, motivo, fechaAccion), "PARADA_FALLIDA");
    }

    @Override
    public void publishNovedadGrave(UUID paqueteId, UUID rutaId, String tipoNovedad, Instant fechaAccion) {
        publicar(NovedadGraveEvent.of(paqueteId, rutaId, tipoNovedad, fechaAccion), "NOVEDAD_GRAVE");
    }

    @Override
    public void publishParadasSinGestionar(UUID rutaId, TipoCierre tipoCierre, List<UUID> paqueteIds) {
        publicar(ParadasSinGestionarEvent.of(rutaId, tipoCierre, paqueteIds, Instant.now()),
                "PARADAS_SIN_GESTIONAR");
    }

    @SuppressWarnings("null") // writeValueAsString nunca retorna null; lanza JsonProcessingException si falla
    private void publicar(Object evento, String tipo) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            sqsTemplate.send(eventosPaqueteQueue, payload);
            log.info("[M1-SQS] {} enviado a {}", tipo, eventosPaqueteQueue);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando evento M1 " + tipo + ": " + evento, e);
        }
    }
}
