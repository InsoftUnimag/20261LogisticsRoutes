package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistics.routes.application.event.RutaCerradaEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void publishRutaCerrada_envia_jsonNode_a_la_cola_de_cierre() {
        UUID rutaId    = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        UUID condId    = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        UUID vehiculoId = UUID.fromString("a0000000-0000-0000-0000-000000000001");

        RutaCerradaEvent event = new RutaCerradaEvent(
                "RUTA_CERRADA", rutaId,
                Instant.parse("2026-04-22T08:00:00Z"),
                Instant.parse("2026-04-22T17:30:00Z"),
                new RutaCerradaEvent.ConductorInfo(condId, "Juan", "Recorrido completo"),
                new RutaCerradaEvent.VehiculoInfo(vehiculoId, "MOTO"),
                List.of()
        );

        adapter.publishRutaCerrada(event);

        // Verifica que se envió un JsonNode (no la clase interna RutaCerradaEvent)
        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());

        JsonNode sent = captor.getValue();

        // Campos principales del contrato con M3
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("RUTA_CERRADA");
        assertThat(sent.get("ruta_id").asText()).isEqualTo(rutaId.toString());

        // Instant serializado como ISO-8601 (prueba que ObjectMapper tiene JavaTimeModule)
        assertThat(sent.get("fecha_hora_cierre").asText()).isEqualTo("2026-04-22T17:30:00Z");

        // snake_case aplicado (prueba que @JsonNaming se respeta)
        assertThat(sent.get("fecha_hora_inicio_transito").asText()).isEqualTo("2026-04-22T08:00:00Z");

        // Conductor con modelo_contrato descriptivo
        assertThat(sent.get("conductor").get("modelo_contrato").asText())
                .isEqualTo("Recorrido completo");
    }
}
