package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.routes.domain.enums.TipoCierre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de serialización JSON de los eventos hacia Módulo 1 (SPEC-08 sección 3).
 * Verifica que el payload usa snake_case y respeta la estructura del contrato.
 */
class Modulo1EventsSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void paquete_en_transito_serializa_segun_spec08_evento1() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T10:00:00Z");

        String json = objectMapper.writeValueAsString(
                PaqueteEnTransitoEvent.of(paqueteId, rutaId, fecha));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("PAQUETE_EN_TRANSITO");
        assertThat(node.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T10:00:00Z");
    }

    @Test
    void paquete_entregado_serializa_con_evidencia_segun_spec08_evento2() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T11:30:00Z");

        String json = objectMapper.writeValueAsString(PaqueteEntregadoEvent.of(
                paqueteId, rutaId, fecha,
                "https://s3/foto.jpg", "https://s3/firma.png"));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("PAQUETE_ENTREGADO");
        assertThat(node.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T11:30:00Z");
        assertThat(node.get("evidencia").get("url_foto").asText()).isEqualTo("https://s3/foto.jpg");
        assertThat(node.get("evidencia").get("url_firma").asText()).isEqualTo("https://s3/firma.png");
    }

    @Test
    void parada_fallida_serializa_con_motivo_segun_spec08_evento3() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T12:00:00Z");

        String json = objectMapper.writeValueAsString(
                ParadaFallidaEvent.of(paqueteId, rutaId, "CLIENTE_AUSENTE", fecha));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("PARADA_FALLIDA");
        assertThat(node.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T12:00:00Z");
        assertThat(node.get("motivo").asText()).isEqualTo("CLIENTE_AUSENTE");
    }

    @Test
    void novedad_grave_serializa_con_tipo_novedad_segun_spec08_evento4() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T13:00:00Z");

        String json = objectMapper.writeValueAsString(
                NovedadGraveEvent.of(paqueteId, rutaId, "DAÑADO_EN_RUTA", fecha));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("NOVEDAD_GRAVE");
        assertThat(node.get("tipo_novedad").asText()).isEqualTo("DAÑADO_EN_RUTA");
    }

    @Test
    void paradas_sin_gestionar_serializa_lista_de_paquetes_segun_spec08_evento5() throws Exception {
        UUID rutaId = UUID.randomUUID();
        UUID paquete1 = UUID.randomUUID();
        UUID paquete2 = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T14:00:00Z");

        String json = objectMapper.writeValueAsString(ParadasSinGestionarEvent.of(
                rutaId, TipoCierre.AUTOMATICO, List.of(paquete1, paquete2), fecha));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("PARADAS_SIN_GESTIONAR");
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("tipo_cierre").asText()).isEqualTo("AUTOMATICO");
        assertThat(node.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T14:00:00Z");
        assertThat(node.get("paquetes")).hasSize(2);
        assertThat(node.get("paquetes").get(0).get("paquete_id").asText()).isEqualTo(paquete1.toString());
        assertThat(node.get("paquetes").get(1).get("paquete_id").asText()).isEqualTo(paquete2.toString());
    }

    @Test
    void paquete_excluido_despacho_serializa_segun_spec08_evento6() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T09:00:00Z");

        String json = objectMapper.writeValueAsString(
                PaqueteExcluidoDespachoEvent.of(paqueteId, rutaId, fecha));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("tipo_evento").asText()).isEqualTo("PAQUETE_EXCLUIDO_DESPACHO");
        assertThat(node.get("paquete_id").asText()).isEqualTo(paqueteId.toString());
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("fecha_hora_evento").asText()).isEqualTo("2026-04-24T09:00:00Z");
    }
}
