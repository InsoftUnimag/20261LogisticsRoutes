package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.logistics.routes.application.event.RutaCerradaEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsIntegracionModulo3AdapterTest {

    private static final String QUEUE = "cierre-ruta-queue";

    @Mock SqsTemplate sqsTemplate;
    SqsIntegracionModulo3Adapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SqsIntegracionModulo3Adapter(sqsTemplate);
        ReflectionTestUtils.setField(adapter, "cierreRutaQueue", QUEUE);
    }

    @Test
    void publishRutaCerrada_envia_evento_completo_a_la_cola_de_cierre() {
        RutaCerradaEvent event = new RutaCerradaEvent(
                "RUTA_CERRADA", UUID.randomUUID(), "MANUAL",
                Instant.parse("2026-04-22T08:00:00Z"),
                Instant.parse("2026-04-22T17:30:00Z"),
                new RutaCerradaEvent.ConductorInfo(UUID.randomUUID(), "Juan", "RECORRIDO_COMPLETO"),
                new RutaCerradaEvent.VehiculoInfo(UUID.randomUUID(), "MNT478", "MOTO"),
                List.of()
        );

        adapter.publishRutaCerrada(event);

        verify(sqsTemplate).send(eq(QUEUE), eq(event));
    }
}
