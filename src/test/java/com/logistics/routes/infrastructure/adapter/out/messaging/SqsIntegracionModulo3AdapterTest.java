package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.routes.application.event.RutaCerradaEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null") // captor.capture() es @Nullable según el tipo pero nunca null en contexto Mockito
@ExtendWith(MockitoExtension.class)
class SqsIntegracionModulo3AdapterTest {

    private static final String QUEUE = "cierre-ruta-queue";

    @Mock SqsTemplate sqsTemplate;
    SqsIntegracionModulo3Adapter adapter;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new SqsIntegracionModulo3Adapter(sqsTemplate, objectMapper, QUEUE);
    }

    @Test
    void publishRutaCerrada_envia_evento_completo_a_la_cola_de_cierre() throws Exception {
        UUID rutaId = UUID.randomUUID();
        RutaCerradaEvent event = new RutaCerradaEvent(
                "RUTA_CERRADA", rutaId, "MANUAL",
                Instant.parse("2026-04-22T08:00:00Z"),
                Instant.parse("2026-04-22T17:30:00Z"),
                new RutaCerradaEvent.ConductorInfo(UUID.randomUUID(), "Juan", "RECORRIDO_COMPLETO"),
                new RutaCerradaEvent.VehiculoInfo(UUID.randomUUID(), "MNT478", "MOTO"),
                List.of()
        );

        adapter.publishRutaCerrada(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        RutaCerradaEvent enviado = objectMapper.readValue(captor.getValue(), RutaCerradaEvent.class);
        assertThat(enviado.tipoEvento()).isEqualTo("RUTA_CERRADA");
        assertThat(enviado.rutaId()).isEqualTo(rutaId);
        assertThat(enviado.tipoCierre()).isEqualTo("MANUAL");
        assertThat(enviado.conductor().nombre()).isEqualTo("Juan");
    }
}
