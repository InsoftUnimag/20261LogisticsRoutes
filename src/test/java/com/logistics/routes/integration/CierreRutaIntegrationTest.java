package com.logistics.routes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.infrastructure.dto.request.CierreRutaRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CierreRutaIntegrationTest {

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
    @Autowired ConductorRepositoryPort conductorRepo;
    @Autowired VehiculoRepositoryPort vehiculoRepo;
    @Autowired RutaRepositoryPort rutaRepo;
    @Autowired ParadaRepositoryPort paradaRepo;

    // ── Helpers de seedeo ─────────────────────────────────────────────────────

    private Conductor crearConductor() {
        String email = "conductor-" + UUID.randomUUID() + "@test.com";
        Conductor c = Conductor.reconstituir(UUID.randomUUID(), "Test Conductor", email,
                ModeloContrato.RECORRIDO_COMPLETO, EstadoConductor.EN_RUTA, null);
        return conductorRepo.guardar(c);
    }

    private Vehiculo crearVehiculo(UUID conductorId) {
        Vehiculo v = Vehiculo.reconstituir(UUID.randomUUID(), "PLK-" + UUID.randomUUID().toString().substring(0, 4),
                TipoVehiculo.VAN, "Transit", 300.0, 5.0, "d3gpz",
                EstadoVehiculo.EN_TRANSITO, conductorId);
        return vehiculoRepo.guardar(v);
    }

    private Ruta crearRutaEnTransito(UUID conductorId, UUID vehiculoId) {
        Ruta r = Ruta.reconstituir(UUID.randomUUID(), "d3gpz", EstadoRuta.EN_TRANSITO, 120.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minus(5, ChronoUnit.DAYS),
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().minus(3, ChronoUnit.DAYS),
                null, null);
        return rutaRepo.guardar(r);
    }

    private Parada crearParadaPendiente(UUID rutaId) {
        Parada p = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 1,
                "Calle 10 #5-20", 4.71, -74.07, null, null,
                Instant.now().plus(1, ChronoUnit.DAYS),
                EstadoParada.PENDIENTE, null, null, null, null, null, OrigenParada.SISTEMA);
        return paradaRepo.guardar(p);
    }

    private Parada crearParadaExitosa(UUID rutaId) {
        Parada p = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 2,
                "Calle 11 #6-30", 4.72, -74.08, null, null,
                Instant.now().plus(1, ChronoUnit.DAYS),
                EstadoParada.EXITOSA, null, Instant.now().minus(1, ChronoUnit.HOURS),
                "firma.jpg", "foto.jpg", "Receptor", OrigenParada.CONDUCTOR);
        return paradaRepo.guardar(p);
    }

    // ── Conductor cierra ruta sin paradas pendientes ──────────────────────────

    @Test
    @WithMockUser(roles = "DRIVER")
    void conductor_cierra_ruta_sin_pendientes_devuelve_204() throws Exception {
        Conductor conductor = crearConductor();
        Vehiculo vehiculo = crearVehiculo(conductor.getId());
        Ruta ruta = crearRutaEnTransito(conductor.getId(), vehiculo.getId());
        crearParadaExitosa(ruta.getId());

        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", ruta.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(false))))
                .andExpect(status().isNoContent());

        Ruta cerrada = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(cerrada.getEstado()).isEqualTo(EstadoRuta.CERRADA_MANUAL);
        assertThat(cerrada.getFechaHoraCierre()).isNotNull();

        Vehiculo liberado = vehiculoRepo.buscarPorId(vehiculo.getId()).orElseThrow();
        assertThat(liberado.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);

        Conductor activo = conductorRepo.buscarPorId(conductor.getId()).orElseThrow();
        assertThat(activo.getEstado()).isEqualTo(EstadoConductor.ACTIVO);
    }

    // ── Conductor rechaza cierre con paradas pendientes sin confirmar ─────────

    @Test
    @WithMockUser(roles = "DRIVER")
    void conductor_cierre_con_pendientes_sin_confirmar_devuelve_409() throws Exception {
        Conductor conductor = crearConductor();
        Vehiculo vehiculo = crearVehiculo(conductor.getId());
        Ruta ruta = crearRutaEnTransito(conductor.getId(), vehiculo.getId());
        crearParadaPendiente(ruta.getId());

        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", ruta.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARADAS_PENDIENTES"));

        Ruta sinCerrar = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(sinCerrar.getEstado()).isEqualTo(EstadoRuta.EN_TRANSITO);
    }

    // ── Conductor confirma cierre marcando pendientes SIN_GESTION ─────────────

    @Test
    @WithMockUser(roles = "DRIVER")
    void conductor_confirma_cierre_marca_pendientes_sin_gestion() throws Exception {
        Conductor conductor = crearConductor();
        Vehiculo vehiculo = crearVehiculo(conductor.getId());
        Ruta ruta = crearRutaEnTransito(conductor.getId(), vehiculo.getId());
        Parada pendiente = crearParadaPendiente(ruta.getId());

        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", ruta.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(true))))
                .andExpect(status().isNoContent());

        Ruta cerrada = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(cerrada.getEstado()).isEqualTo(EstadoRuta.CERRADA_MANUAL);

        Parada sinGestion = paradaRepo.buscarPorId(pendiente.getId()).orElseThrow();
        assertThat(sinGestion.getEstado()).isEqualTo(EstadoParada.SIN_GESTION_CONDUCTOR);
        assertThat(sinGestion.getOrigen()).isEqualTo(OrigenParada.SISTEMA);
        assertThat(sinGestion.getFechaHoraGestion()).isNotNull();
    }

    // ── Despachador fuerza cierre ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void despachador_fuerza_cierre_devuelve_204_y_transiciona_a_cerrada_forzada() throws Exception {
        Conductor conductor = crearConductor();
        Vehiculo vehiculo = crearVehiculo(conductor.getId());
        Ruta ruta = crearRutaEnTransito(conductor.getId(), vehiculo.getId());
        crearParadaPendiente(ruta.getId());

        mockMvc.perform(post("/api/despacho/rutas/{id}/forzar-cierre", ruta.getId()))
                .andExpect(status().isNoContent());

        Ruta cerrada = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(cerrada.getEstado()).isEqualTo(EstadoRuta.CERRADA_FORZADA);
    }

    // ── Ruta inexistente → 404 ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "DRIVER")
    void cierre_de_ruta_inexistente_devuelve_404() throws Exception {
        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RUTA_NO_ENCONTRADA"));
    }
}
