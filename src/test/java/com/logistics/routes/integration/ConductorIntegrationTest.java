package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.infrastructure.dto.request.AsignacionRequest;
import com.logistics.routes.infrastructure.dto.request.ConductorRequest;
import com.logistics.routes.infrastructure.dto.request.VehiculoRequest;
import com.logistics.routes.infrastructure.dto.response.ConductorResponse;
import com.logistics.routes.infrastructure.dto.response.VehiculoResponse;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = {"FLEET_ADMIN", "DISPATCHER"})
class ConductorIntegrationTest {

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

    private static final String CONDUCTORES = "/api/conductores";
    private static final String VEHICULOS   = "/api/vehiculos";

    // ── Test 1 ───────────────────────────────────────────────────────────────

    @Test
    void registrar_conductor_retorna_201_activo_sin_vehiculo() throws Exception {
        ConductorRequest req = new ConductorRequest(
                "Luis Hernández", "luis.hernandez@logisticasm.com", ModeloContrato.POR_PARADA);

        mockMvc.perform(post(CONDUCTORES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.vehiculoAsignadoId").doesNotExist());
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    @Test
    void email_duplicado_retorna_409() throws Exception {
        ConductorRequest req = new ConductorRequest(
                "Pedro García", "pedro.dup@logisticasm.com", ModeloContrato.POR_PARADA);
        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post(CONDUCTORES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CONDUCTORES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"));
    }

    // ── Test 3 (Checkpoint principal) ────────────────────────────────────────

    @Test
    void asignar_desvincular_historial_tiene_fecha_fin_completa() throws Exception {
        // 1. Crear conductor
        ConductorRequest conductorReq = new ConductorRequest(
                "María López", "maria.asign@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO);
        MvcResult conductorResult = mockMvc.perform(post(CONDUCTORES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conductorReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ConductorResponse conductor = objectMapper.readValue(
                conductorResult.getResponse().getContentAsString(), ConductorResponse.class);

        // 2. Crear vehículo disponible
        VehiculoRequest vehiculoReq = new VehiculoRequest(
                "ASG001", TipoVehiculo.MOTO, "Honda CB 160F",
                BigDecimal.valueOf(35), BigDecimal.valueOf(0.35), "d2g");
        MvcResult vehiculoResult = mockMvc.perform(post(VEHICULOS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehiculoReq)))
                .andExpect(status().isCreated())
                .andReturn();
        VehiculoResponse vehiculo = objectMapper.readValue(
                vehiculoResult.getResponse().getContentAsString(), VehiculoResponse.class);

        // 3. Asignar conductor → vehículo
        AsignacionRequest asignReq = new AsignacionRequest(vehiculo.id());
        mockMvc.perform(post(CONDUCTORES + "/" + conductor.id() + "/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiculoAsignadoId").value(vehiculo.id().toString()));

        // 4. Desvincular
        mockMvc.perform(delete(CONDUCTORES + "/" + conductor.id() + "/asignacion"))
                .andExpect(status().isNoContent());

        // 5. Consultar historial — debe haber 1 registro con fechaHoraFin no nula
        MvcResult historialResult = mockMvc.perform(get(CONDUCTORES + "/" + conductor.id() + "/historial"))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> historial = objectMapper.readValue(
                historialResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).get("activo")).isEqualTo(false);
        assertThat(historial.get(0).get("fechaHoraFin")).isNotNull();
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    @Test
    void dar_de_baja_conductor_sin_vehiculo_retorna_204() throws Exception {
        ConductorRequest req = new ConductorRequest(
                "Carlos Baja", "carlos.baja@logisticasm.com", ModeloContrato.POR_PARADA);
        MvcResult created = mockMvc.perform(post(CONDUCTORES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        ConductorResponse conductor = objectMapper.readValue(
                created.getResponse().getContentAsString(), ConductorResponse.class);

        mockMvc.perform(delete(CONDUCTORES + "/" + conductor.id()))
                .andExpect(status().isNoContent());
    }
}
