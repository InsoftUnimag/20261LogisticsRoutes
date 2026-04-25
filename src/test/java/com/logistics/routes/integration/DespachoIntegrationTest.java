package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.infrastructure.dto.request.ConfirmarDespachoRequest;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "DISPATCHER")
class DespachoIntegrationTest {

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
    @Autowired RutaRepositoryPort rutaRepository;
    @Autowired ParadaRepositoryPort paradaRepository;

    private static final String BASE = "/api/despacho";

    // IDs del seed V2__seed.sql
    private static final UUID CONDUCTOR_JUAN_CARLOS =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
    private static final UUID VEHICULO_MNT478_MOTO =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    // ── Helper ───────────────────────────────────────────────────────────────

    private Ruta rutaListaParaDespacho(String zonaHash) {
        Ruta ruta = Ruta.nueva(zonaHash, Instant.now().plus(5, ChronoUnit.DAYS));
        ruta.transicionarAListaParaDespacho();
        return rutaRepository.guardar(ruta);
    }

    // ── Test 1: GET /rutas ────────────────────────────────────────────────────

    @Test
    void listar_incluye_rutas_en_lista_para_despacho() throws Exception {
        Ruta ruta = rutaListaParaDespacho("d29eb");

        mockMvc.perform(get(BASE + "/rutas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + ruta.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id == '" + ruta.getId() + "')].estado")
                        .value(org.hamcrest.Matchers.hasItem("LISTA_PARA_DESPACHO")));
    }

    // ── Test 2: POST /rutas/{id}/confirmar (flujo feliz) ─────────────────────

    @Test
    void confirmar_ruta_transiciona_a_confirmada_y_optimiza_paradas() throws Exception {
        // Given: ruta LISTA_PARA_DESPACHO con 2 paradas desordenadas
        Ruta ruta = rutaListaParaDespacho("d29ec");
        paradaRepository.guardar(Parada.nueva(ruta.getId(), UUID.randomUUID(),
                "Dir A", 11.240, -74.200, null, null, null));
        paradaRepository.guardar(Parada.nueva(ruta.getId(), UUID.randomUUID(),
                "Dir B", 11.241, -74.201, null, null, null));

        String body = objectMapper.writeValueAsString(
                new ConfirmarDespachoRequest(CONDUCTOR_JUAN_CARLOS, VEHICULO_MNT478_MOTO));

        // When
        mockMvc.perform(post(BASE + "/rutas/" + ruta.getId() + "/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.conductorId").value(CONDUCTOR_JUAN_CARLOS.toString()))
                .andExpect(jsonPath("$.vehiculoId").value(VEHICULO_MNT478_MOTO.toString()))
                .andExpect(jsonPath("$.cantidadParadas").value(2));

        // Then: estado persistido en BD
        Ruta persistida = rutaRepository.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoRuta.CONFIRMADA);
        assertThat(persistida.getConductorId()).isEqualTo(CONDUCTOR_JUAN_CARLOS);
        assertThat(persistida.getVehiculoId()).isEqualTo(VEHICULO_MNT478_MOTO);
    }

    // ── Test 3: POST /rutas/{id}/confirmar con ruta inexistente ──────────────

    @Test
    void confirmar_ruta_inexistente_retorna_404() throws Exception {
        UUID rutaInexistente = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new ConfirmarDespachoRequest(UUID.randomUUID(), UUID.randomUUID()));

        mockMvc.perform(post(BASE + "/rutas/" + rutaInexistente + "/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"));
    }

    // ── Test 4: DELETE /rutas/{id}/paquetes/{paqueteId} ──────────────────────

    @Test
    void excluir_paquete_cambia_estado_parada_y_retorna_204() throws Exception {
        Ruta ruta = rutaListaParaDespacho("d29ed");
        UUID paqueteId = UUID.randomUUID();
        paradaRepository.guardar(Parada.nueva(ruta.getId(), paqueteId,
                "Dir X", 11.240, -74.200, null, null, null));

        mockMvc.perform(delete(BASE + "/rutas/" + ruta.getId() + "/paquetes/" + paqueteId)
                        .param("motivo", "paquete dañado en centro de acopio"))
                .andExpect(status().isNoContent());

        Parada persistida = paradaRepository.buscarPorRutaYPaquete(ruta.getId(), paqueteId).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoParada.EXCLUIDA_DESPACHO);
    }

    // ── Test 5: DELETE con parada inexistente ────────────────────────────────

    @Test
    void excluir_parada_inexistente_retorna_404() throws Exception {
        UUID rutaId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();

        mockMvc.perform(delete(BASE + "/rutas/" + rutaId + "/paquetes/" + paqueteId)
                        .param("motivo", "cualquiera"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PARADA_NO_ENCONTRADA"));
    }

    // ── Test 6: POST /rutas/{id}/forzar-cierre con ruta inexistente ──────────

    @Test
    void forzar_cierre_de_ruta_inexistente_retorna_404() throws Exception {
        mockMvc.perform(post(BASE + "/rutas/" + UUID.randomUUID() + "/forzar-cierre"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"));
    }
}
