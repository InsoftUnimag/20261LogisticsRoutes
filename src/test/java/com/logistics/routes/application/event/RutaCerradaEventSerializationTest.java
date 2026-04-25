package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de serialización JSON del evento RUTA_CERRADA hacia Módulo 3
 * (SPEC-08 sección 4). Verifica payload completo y snake_case.
 */
class RutaCerradaEventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void ruta_cerrada_serializa_payload_completo_segun_spec08_seccion4() throws Exception {
        UUID rutaId = UUID.randomUUID();
        UUID conductorId = UUID.randomUUID();
        UUID vehiculoId = UUID.randomUUID();
        UUID paquete1 = UUID.randomUUID();
        UUID paquete2 = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-04-22T08:00:00Z");
        Instant cierre = Instant.parse("2026-04-22T17:30:00Z");
        Instant gestion = Instant.parse("2026-04-22T15:00:00Z");

        RutaCerradaEvent event = new RutaCerradaEvent(
                "RUTA_CERRADA",
                rutaId,
                "MANUAL",
                inicio,
                cierre,
                new RutaCerradaEvent.ConductorInfo(conductorId, "Juan Pérez", "RECORRIDO_COMPLETO"),
                new RutaCerradaEvent.VehiculoInfo(vehiculoId, "MNT478", "MOTO"),
                List.of(
                        new RutaCerradaEvent.ParadaInfo(paquete1, "EXITOSA", null, gestion),
                        new RutaCerradaEvent.ParadaInfo(paquete2, "FALLIDA", "CLIENTE_AUSENTE", gestion)
                )
        );

        String json = objectMapper.writeValueAsString(event);
        JsonNode node = objectMapper.readTree(json);

        // Campos top-level
        assertThat(node.get("tipo_evento").asText()).isEqualTo("RUTA_CERRADA");
        assertThat(node.get("ruta_id").asText()).isEqualTo(rutaId.toString());
        assertThat(node.get("tipo_cierre").asText()).isEqualTo("MANUAL");
        assertThat(node.get("fecha_hora_inicio_transito").asText()).isEqualTo("2026-04-22T08:00:00Z");
        assertThat(node.get("fecha_hora_cierre").asText()).isEqualTo("2026-04-22T17:30:00Z");

        // Conductor
        JsonNode conductor = node.get("conductor");
        assertThat(conductor.get("conductor_id").asText()).isEqualTo(conductorId.toString());
        assertThat(conductor.get("nombre").asText()).isEqualTo("Juan Pérez");
        assertThat(conductor.get("modelo_contrato").asText()).isEqualTo("RECORRIDO_COMPLETO");

        // Vehiculo
        JsonNode vehiculo = node.get("vehiculo");
        assertThat(vehiculo.get("vehiculo_id").asText()).isEqualTo(vehiculoId.toString());
        assertThat(vehiculo.get("placa").asText()).isEqualTo("MNT478");
        assertThat(vehiculo.get("tipo").asText()).isEqualTo("MOTO");

        // Paradas
        assertThat(node.get("paradas")).hasSize(2);
        JsonNode p1 = node.get("paradas").get(0);
        assertThat(p1.get("paquete_id").asText()).isEqualTo(paquete1.toString());
        assertThat(p1.get("estado").asText()).isEqualTo("EXITOSA");
        assertThat(p1.get("motivo_no_entrega").isNull()).isTrue();
        assertThat(p1.get("fecha_hora_gestion").asText()).isEqualTo("2026-04-22T15:00:00Z");

        JsonNode p2 = node.get("paradas").get(1);
        assertThat(p2.get("estado").asText()).isEqualTo("FALLIDA");
        assertThat(p2.get("motivo_no_entrega").asText()).isEqualTo("CLIENTE_AUSENTE");
    }
}
