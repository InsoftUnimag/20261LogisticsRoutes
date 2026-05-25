package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistics.routes.domain.enums.TipoCierre;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsIntegracionModulo1AdapterTest {

    private static final String EVENTOS_QUEUE = "eventos-paquete-queue";
    private static final String RESPUESTAS_QUEUE = "respuestas-ruta-queue";

    @Mock SqsTemplate sqsTemplate;

    SqsIntegracionModulo1Adapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SqsIntegracionModulo1Adapter(sqsTemplate);
        ReflectionTestUtils.setField(adapter, "eventosPaqueteQueue", EVENTOS_QUEUE);
        ReflectionTestUtils.setField(adapter, "respuestasRutaQueue", RESPUESTAS_QUEUE);
    }

    @Test
    void publishRutaAsignada_envia_jsonNode_a_la_cola_de_respuestas() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T10:00:00Z");

        adapter.publishRutaAsignada(paqueteId, rutaId, fecha);

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(RESPUESTAS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("RUTA_ASIGNADA");
        assertThat(sent.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(sent.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(sent.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T10:00:00Z");
    }

    @Test
    void publishPaqueteEnTransito_envia_jsonNode_a_la_cola_correcta() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T10:00:00Z");

        adapter.publishPaqueteEnTransito(paqueteId, rutaId, fecha);

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("PAQUETE_EN_TRANSITO");
        assertThat(sent.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(sent.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(sent.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T10:00:00Z");
    }

    @Test
    void publishPaqueteEntregado_envia_jsonNode_con_evidencia() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T11:30:00Z");

        adapter.publishPaqueteEntregado(paqueteId, rutaId, fecha,
                "https://s3/foto.jpg", "https://s3/firma.png");

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("PAQUETE_ENTREGADO");
        assertThat(sent.get("evidencia").get("url_foto").asText()).isEqualTo("https://s3/foto.jpg");
        assertThat(sent.get("evidencia").get("url_firma").asText()).isEqualTo("https://s3/firma.png");
    }

    @Test
    void publishParadaFallida_envia_jsonNode_con_motivo() {
        adapter.publishParadaFallida(UUID.randomUUID(), UUID.randomUUID(),
                "CLIENTE_AUSENTE", Instant.now());

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("PARADA_FALLIDA");
        assertThat(sent.get("motivo").asText()).isEqualTo("CLIENTE_AUSENTE");
    }

    @Test
    void publishNovedadGrave_envia_jsonNode_con_tipo_novedad() {
        adapter.publishNovedadGrave(UUID.randomUUID(), UUID.randomUUID(),
                "DAÑADO_EN_RUTA", Instant.now());

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("NOVEDAD_GRAVE");
        assertThat(sent.get("tipo_novedad").asText()).isEqualTo("DAÑADO_EN_RUTA");
    }

    @Test
    void publishParadasSinGestionar_envia_jsonNode_con_lista_de_paquetes() {
        UUID rutaId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        adapter.publishParadasSinGestionar(rutaId, TipoCierre.AUTOMATICO, List.of(p1, p2));

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("PARADAS_SIN_GESTIONAR");
        assertThat(sent.get("tipo_cierre").asText()).isEqualTo("AUTOMATICO");
        JsonNode paquetes = sent.get("paquetes");
        assertThat(paquetes.isArray()).isTrue();
        assertThat(paquetes).hasSize(2);
        assertThat(paquetes.get(0).get("paquete_id").asText()).isEqualTo(p1.toString());
        assertThat(paquetes.get(1).get("paquete_id").asText()).isEqualTo(p2.toString());
    }

    @Test
    void publishPaqueteExcluidoDespacho_envia_jsonNode_con_ids() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();

        adapter.publishPaqueteExcluidoDespacho(paqueteId, rutaId, "motivo libre", Instant.now());

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), captor.capture());
        JsonNode sent = captor.getValue();
        assertThat(sent.get("tipo_evento").asText()).isEqualTo("PAQUETE_EXCLUIDO_DESPACHO");
        assertThat(sent.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(sent.get("ruta_id").asText()).isEqualTo(rutaId.toString());
    }

    @Test
    void no_envia_a_otra_cola_diferente_de_la_configurada() {
        adapter.publishPaqueteEnTransito(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        verify(sqsTemplate).send(eq(EVENTOS_QUEUE), any(JsonNode.class));
    }
}
