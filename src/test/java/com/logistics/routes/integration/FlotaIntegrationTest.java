package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.infrastructure.dto.request.AsignacionRequest;
import com.logistics.routes.infrastructure.dto.request.ConductorRequest;
import com.logistics.routes.infrastructure.dto.request.VehiculoRequest;
import com.logistics.routes.infrastructure.dto.response.ConductorResponse;
import com.logistics.routes.infrastructure.dto.response.FlotaDisponibilidadResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = {"FLEET_ADMIN", "DISPATCHER"})
class FlotaIntegrationTest {

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

    @Test
    void flujo_consolidado_gestion_de_flota() throws Exception {
        // 1. Registrar vehículo
        VehiculoRequest vehiculoReq = new VehiculoRequest(
                "FLT123", TipoVehiculo.VAN, "Renault Kangoo",
                BigDecimal.valueOf(800), BigDecimal.valueOf(3.0), "d3gpz");
        
        MvcResult vehiculoResult = mockMvc.perform(post(VEHICULOS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehiculoReq)))
                .andExpect(status().isCreated())
                .andReturn();
        
        VehiculoResponse vehiculo = objectMapper.readValue(
                vehiculoResult.getResponse().getContentAsString(), VehiculoResponse.class);

        // 2. Registrar conductor
        ConductorRequest conductorReq = new ConductorRequest(
                "Ana Martínez", "ana.martinez@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO);
        
        MvcResult conductorResult = mockMvc.perform(post(CONDUCTORES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conductorReq)))
                .andExpect(status().isCreated())
                .andReturn();
        
        ConductorResponse conductor = objectMapper.readValue(
                conductorResult.getResponse().getContentAsString(), ConductorResponse.class);

        // 3. Asignar conductor a vehículo
        AsignacionRequest asignReq = new AsignacionRequest(vehiculo.id());
        mockMvc.perform(post(CONDUCTORES + "/" + conductor.id() + "/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asignReq)))
                .andExpect(status().isOk());

        // 4. Consultar disponibilidad del panel
        MvcResult disponibilidadResult = mockMvc.perform(get(VEHICULOS + "/disponibilidad"))
                .andExpect(status().isOk())
                .andReturn();
        
        List<FlotaDisponibilidadResponse> disponibilidad = objectMapper.readValue(
                disponibilidadResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FlotaDisponibilidadResponse.class));
        
        boolean isPresentInDisponibilidad = disponibilidad.stream()
                .anyMatch(d -> d.id().equals(vehiculo.id()) && d.conductorId() != null);
        assertThat(isPresentInDisponibilidad).isTrue();

        // 5. Desvincular conductor
        mockMvc.perform(delete(CONDUCTORES + "/" + conductor.id() + "/asignacion"))
                .andExpect(status().isNoContent());

        // 6. Dar de baja vehículo
        mockMvc.perform(delete(VEHICULOS + "/" + vehiculo.id()))
                .andExpect(status().isNoContent());

        // 7. Dar de baja conductor
        mockMvc.perform(delete(CONDUCTORES + "/" + conductor.id()))
                .andExpect(status().isNoContent());
    }
}
