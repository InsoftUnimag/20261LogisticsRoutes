package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.logistics.routes.application.event.NovedadGraveEvent;
import com.logistics.routes.application.event.PaqueteEnTransitoEvent;
import com.logistics.routes.application.event.PaqueteEntregadoEvent;
import com.logistics.routes.application.event.PaqueteExcluidoDespachoEvent;
import com.logistics.routes.application.event.ParadaFallidaEvent;
import com.logistics.routes.application.event.ParadasSinGestionarEvent;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.domain.enums.TipoCierre;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementación AWS SQS del puerto hacia Módulo 1.
 * Publica los eventos del ciclo de vida de paquetes (SPEC-08 sección 3) en la
 * cola configurada {@code app.sqs.eventos-paquete-queue}.
 * Solo activo bajo el perfil {@code aws}.
 */
@Component
@Profile("aws")
@RequiredArgsConstructor
public class SqsIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegracionModulo1Adapter.class);

    private final SqsTemplate sqsTemplate;

    @Value("${app.sqs.eventos-paquete-queue}")
    private String eventosPaqueteQueue;

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

    private void publicar(Object evento, String tipo) {
        sqsTemplate.send(eventosPaqueteQueue, evento);
        log.info("[M1-SQS] {} enviado a {}", tipo, eventosPaqueteQueue);
    }
}
