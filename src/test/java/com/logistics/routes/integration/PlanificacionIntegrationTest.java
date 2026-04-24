package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.infrastructure.dto.request.SolicitarRutaRequest;
import com.logistics.routes.infrastructure.dto.response.SolicitarRutaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "SYSTEM")
class PlanificacionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String BASE = "/api/planificacion";

    // Coordenadas en Bogotá → zona "d29ej"
    private static final double LAT = 4.7109;
    private static final double LON = -74.0721;

    // Coordenadas en Medellín → zona diferente
    private static final double LAT_MDE = 6.2518;
    private static final double LON_MDE = -75.5636;

    private SolicitarRutaRequest requestValido(double pesoKg, double lat, double lon) {
        return new SolicitarRutaRequest(
                UUID.randomUUID(), pesoKg, lat, lon,
                "Calle 72 #10-25, Bogotá",
                "GENERAL", "EFECTIVO",
                Instant.now().plus(3, ChronoUnit.DAYS)
        );
    }

    // ── Test 1 ───────────────────────────────────────────────────────────────

    @Test
    void solicitar_ruta_retorna_200_con_ruta_id() throws Exception {
        // Given: primer paquete de la zona
        String body = objectMapper.writeValueAsString(requestValido(5.0, LAT, LON));

        // When: POST /api/planificacion/solicitar-ruta
        mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Then: 200 OK con rutaId no nulo
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaId").isNotEmpty());
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    @Test
    void segunda_solicitud_en_misma_zona_retorna_la_misma_ruta() throws Exception {
        // Given: primera solicitud crea la ruta en la zona
        String body1 = objectMapper.writeValueAsString(requestValido(5.0, LAT, LON));
        MvcResult result1 = mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andReturn();

        SolicitarRutaResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), SolicitarRutaResponse.class);
        UUID rutaId1 = response1.rutaId();

        // When: segunda solicitud en las mismas coordenadas
        String body2 = objectMapper.writeValueAsString(requestValido(5.0, LAT, LON));
        MvcResult result2 = mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andReturn();

        SolicitarRutaResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), SolicitarRutaResponse.class);

        // Then: misma ruta consolidando ambos paquetes
        assertThat(response2.rutaId()).isEqualTo(rutaId1);
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────

    @Test
    void solicitudes_en_zonas_distintas_crean_rutas_distintas() throws Exception {
        // Given: primera solicitud en Bogotá
        String bodyBog = objectMapper.writeValueAsString(requestValido(5.0, LAT, LON));
        MvcResult resBog = mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBog))
                .andExpect(status().isOk())
                .andReturn();

        // When: segunda solicitud en Medellín (zona diferente)
        String bodyMde = objectMapper.writeValueAsString(requestValido(5.0, LAT_MDE, LON_MDE));
        MvcResult resMde = mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMde))
                .andExpect(status().isOk())
                .andReturn();

        UUID rutaBog = objectMapper.readValue(
                resBog.getResponse().getContentAsString(), SolicitarRutaResponse.class).rutaId();
        UUID rutaMde = objectMapper.readValue(
                resMde.getResponse().getContentAsString(), SolicitarRutaResponse.class).rutaId();

        // Then: rutas distintas para zonas distintas
        assertThat(rutaMde).isNotEqualTo(rutaBog);
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    @Test
    void solicitar_ruta_con_fecha_limite_vencida_retorna_422() throws Exception {
        // Given: paquete con fechaLimiteEntrega en el pasado
        SolicitarRutaRequest request = new SolicitarRutaRequest(
                UUID.randomUUID(), 5.0, LAT, LON,
                "Calle 72 #10-25, Bogotá", "GENERAL", "EFECTIVO",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );
        String body = objectMapper.writeValueAsString(request);

        // When / Then: 422 Unprocessable Entity con código FECHA_LIMITE_VENCIDA
        mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FECHA_LIMITE_VENCIDA"));
    }

    // ── Test 5 ───────────────────────────────────────────────────────────────

    @Test
    void solicitar_ruta_sin_body_obligatorio_retorna_422() throws Exception {
        // Given: body sin paqueteId (campo obligatorio)
        String body = """
                {
                  "pesoKg": 5.0,
                  "latitud": 4.7109,
                  "longitud": -74.0721,
                  "direccion": "Calle 72"
                }
                """;

        // When / Then: 422 con VALIDATION_ERROR
        mockMvc.perform(post(BASE + "/solicitar-ruta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
    }
}
