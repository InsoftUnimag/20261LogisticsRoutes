package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.infrastructure.dto.request.VehiculoRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "FLEET_ADMIN")
class VehiculoIntegrationTest {

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

    private static final String BASE = "/api/vehiculos";

    //helper
    private VehiculoRequest requestValido(String placa) {
        return new VehiculoRequest(
                placa,
                com.logistics.routes.domain.enums.TipoVehiculo.MOTO,
                "Yamaha NMX 155",
                BigDecimal.valueOf(45),
                BigDecimal.valueOf(0.45),
                "d3gpz"
        );
    }

    // ── Test 1 ───────────────────────────────────────────────────────────────

    @Test
    void post_vehiculo_retorna_201_con_id_generado() throws Exception {
        // Given: payload válido con placa nueva (no existe en la semilla V2)
        String body = objectMapper.writeValueAsString(requestValido("TST001"));

        // When: POST /api/vehiculos
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Then: 201 Created con id generado y estado DISPONIBLE
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.placa").value("TST001"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andReturn();

        VehiculoResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VehiculoResponse.class);
        assertThat(response.id()).isNotNull();
        assertThat(response.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    @Test
    void post_vehiculo_con_placa_duplicada_retorna_409() throws Exception {
        // Given: un vehículo con placa DUP999 ya registrado
        String placa = "DUP999";
        String body = objectMapper.writeValueAsString(requestValido(placa));
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // When: se intenta registrar otro vehículo con la misma placa
        // Then: 409 Conflict con código de error PLACA_DUPLICADA
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLACA_DUPLICADA"));
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────

    @Test
    void get_vehiculos_retorna_lista_incluyendo_seed() throws Exception {
        // Given: la migración V2 ya insertó 8 vehículos en la DB de test;
        //        se agrega uno más para asegurar al menos un registro propio
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido("LST111"))))
                .andExpect(status().isCreated());

        // When: GET /api/vehiculos
        MvcResult result = mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andReturn();

        // Then: la lista contiene los 8 de la semilla + el recién creado
        List<?> lista = objectMapper.readValue(
                result.getResponse().getContentAsString(), List.class);
        assertThat(lista.size()).isGreaterThanOrEqualTo(8);
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    @Test
    void delete_vehiculo_disponible_retorna_204_y_queda_inactivo() throws Exception {
        // Given: vehículo recién creado en estado DISPONIBLE
        MvcResult created = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido("DEL777"))))
                .andExpect(status().isCreated())
                .andReturn();
        VehiculoResponse response = objectMapper.readValue(
                created.getResponse().getContentAsString(), VehiculoResponse.class);
        String id = response.id().toString();

        // When: DELETE /api/vehiculos/{id}
        mockMvc.perform(delete(BASE + "/" + id))
                // Then: 204 No Content (soft delete, no se borra físicamente)
                .andExpect(status().isNoContent());

        // Then: el vehículo sigue apareciendo en el listado pero con estado INACTIVO
        MvcResult listResult = mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andReturn();
        String json = listResult.getResponse().getContentAsString();
        assertThat(json).contains("\"id\":\"" + id + "\"");
        assertThat(json).containsPattern("\"id\":\"" + id + "\"[^}]*\"estado\":\"INACTIVO\"");
    }
}
