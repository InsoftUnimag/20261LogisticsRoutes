package com.logistics.routes.infrastructure.adapter.in.messaging;

import com.logistics.routes.application.usecase.SolicitarRutaUseCase;
import com.logistics.routes.infrastructure.dto.request.SolicitarRutaRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Consumer SQS que recibe eventos SOLICITAR_RUTA del Módulo 1 (SGP) y los
 * delega al caso de uso. Solo activo bajo el perfil "aws" — en perfiles dev y
 * test la solicitud llega por el endpoint REST del PlanificacionController.
 */
@Component
@Profile("aws")
@RequiredArgsConstructor
public class SolicitarRutaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SolicitarRutaConsumer.class);

    private final SolicitarRutaUseCase solicitarRuta;

    @SqsListener("${app.sqs.solicitudes-ruta-queue}")
    public void onSolicitarRuta(SolicitarRutaRequest request) {
        log.info("[SQS] SOLICITAR_RUTA recibido: paqueteId={}", request.paqueteId());
        solicitarRuta.ejecutar(request.toCommand());
    }
}
