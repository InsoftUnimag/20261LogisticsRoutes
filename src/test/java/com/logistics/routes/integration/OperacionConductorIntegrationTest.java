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
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.infrastructure.dto.request.CierreRutaRequest;
import com.logistics.routes.infrastructure.dto.request.RegistrarParadaRequest;
import com.logistics.routes.infrastructure.persistence.entity.UsuarioEntity;
import com.logistics.routes.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para el {@link com.logistics.routes.infrastructure.adapter.in.web.ConductorOperacionController}.
 * Cubre los endpoints de operación de campo: consulta de ruta activa, inicio de tránsito,
 * registro de paradas y cierre de ruta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = OperacionConductorIntegrationTest.DRIVER_EMAIL, roles = "DRIVER")
class OperacionConductorIntegrationTest {

    static final String DRIVER_EMAIL = "driver-op-test@test.com";

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
    @Autowired UsuarioJpaRepository usuarioRepo;

    private UUID conductorId;
    private UUID vehiculoId;

    @BeforeEach
    void setUp() {
        Conductor conductor = Conductor.reconstituir(UUID.randomUUID(),
                "Conductor Operación", "op-conductor-" + UUID.randomUUID() + "@test.com",
                ModeloContrato.RECORRIDO_COMPLETO, EstadoConductor.ACTIVO, null);
        conductorId = conductorRepo.guardar(conductor).getId();

        Vehiculo vehiculo = Vehiculo.reconstituir(UUID.randomUUID(),
                "OPK-" + UUID.randomUUID().toString().substring(0, 4),
                TipoVehiculo.VAN, "Transit", 300.0, 5.0, "d3gpz",
                EstadoVehiculo.DISPONIBLE, conductorId);
        vehiculoId = vehiculoRepo.guardar(vehiculo).getId();

        // Vincula el usuario mock al conductor recién creado.
        UsuarioEntity usuario = usuarioRepo.findByEmail(DRIVER_EMAIL)
                .orElseGet(() -> {
                    UsuarioEntity nuevo = new UsuarioEntity();
                    nuevo.setEmail(DRIVER_EMAIL);
                    nuevo.setPasswordHash("$2a$10$placeholder.hash.no.usado.en.tests");
                    nuevo.setRol("DRIVER");
                    return nuevo;
                });
        usuario.setConductorId(conductorId);
        usuarioRepo.save(usuario);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Ruta crearRutaConfirmada() {
        Ruta r = Ruta.reconstituir(UUID.randomUUID(), "d3gpz", EstadoRuta.CONFIRMADA, 80.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null, null, null);
        return rutaRepo.guardar(r);
    }

    private Ruta crearRutaEnTransito() {
        Ruta r = Ruta.reconstituir(UUID.randomUUID(), "d3gpz", EstadoRuta.EN_TRANSITO, 80.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.HOURS), null, null);
        return rutaRepo.guardar(r);
    }

    private Parada crearParadaPendiente(UUID rutaId, int orden) {
        Parada p = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), orden,
                "Calle " + orden + " #5-20", 4.71, -74.07,
                "ESTANDAR", "PREPAGO",
                Instant.now().plus(1, ChronoUnit.DAYS),
                EstadoParada.PENDIENTE, null, null, null, null, null,
                OrigenParada.SISTEMA);
        return paradaRepo.guardar(p);
    }

    // ── GET /ruta-activa ──────────────────────────────────────────────────────

    @Test
    void ruta_activa_sin_asignacion_devuelve_204() throws Exception {
        mockMvc.perform(get("/api/conductor/ruta-activa"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ruta_activa_con_ruta_confirmada_devuelve_detalle_con_paradas() throws Exception {
        Ruta ruta = crearRutaConfirmada();
        crearParadaPendiente(ruta.getId(), 1);
        crearParadaPendiente(ruta.getId(), 2);

        mockMvc.perform(get("/api/conductor/ruta-activa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruta.getId().toString()))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.conductorId").value(conductorId.toString()))
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[0].orden").value(1))
                .andExpect(jsonPath("$.paradas[1].orden").value(2));
    }

    // ── POST /rutas/{id}/iniciar-transito ─────────────────────────────────────

    @Test
    void iniciar_transito_transiciona_ruta_a_en_transito() throws Exception {
        Ruta ruta = crearRutaConfirmada();
        crearParadaPendiente(ruta.getId(), 1);

        mockMvc.perform(post("/api/conductor/rutas/{id}/iniciar-transito", ruta.getId()))
                .andExpect(status().isNoContent());

        Ruta persistida = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoRuta.EN_TRANSITO);
        assertThat(persistida.getFechaHoraInicio()).isNotNull();
    }

    @Test
    void iniciar_transito_de_ruta_ajena_devuelve_403() throws Exception {
        UUID otroConductorId = UUID.randomUUID();
        Conductor otro = Conductor.reconstituir(otroConductorId, "Otro", "otro-" + UUID.randomUUID() + "@test.com",
                ModeloContrato.POR_PARADA, EstadoConductor.ACTIVO, null);
        conductorRepo.guardar(otro);
        Ruta ruta = Ruta.reconstituir(UUID.randomUUID(), "d3gpz", EstadoRuta.CONFIRMADA, 60.0,
                TipoVehiculo.MOTO, vehiculoId, otroConductorId,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null, null, null);
        rutaRepo.guardar(ruta);

        mockMvc.perform(post("/api/conductor/rutas/{id}/iniciar-transito", ruta.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONDUCTOR_NO_ASIGNADO_A_RUTA"));
    }

    @Test
    void iniciar_transito_de_ruta_inexistente_devuelve_404() throws Exception {
        mockMvc.perform(post("/api/conductor/rutas/{id}/iniciar-transito", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"));
    }

    // ── POST /paradas/{paradaId}/registrar ────────────────────────────────────

    @Test
    void registrar_parada_exitosa_actualiza_estado_y_pod() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);
        // Postgres TIMESTAMP solo conserva microsegundos; truncamos para una comparación exacta.
        Instant fechaAccion = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MICROS);

        RegistrarParadaRequest body = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.EXITOSA,
                "https://s3/foto.jpg", "https://s3/firma.png", "Pedro Pérez",
                null, fechaAccion);

        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", parada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        Parada persistida = paradaRepo.buscarPorId(parada.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoParada.EXITOSA);
        assertThat(persistida.getFotoEvidenciaUrl()).isEqualTo("https://s3/foto.jpg");
        assertThat(persistida.getNombreReceptor()).isEqualTo("Pedro Pérez");
        assertThat(persistida.getFechaHoraGestion()).isEqualTo(fechaAccion);
    }

    @Test
    void registrar_parada_exitosa_sin_foto_devuelve_422() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);

        RegistrarParadaRequest body = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.EXITOSA,
                null, null, "Pedro Pérez", null, Instant.now());

        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", parada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());

        Parada persistida = paradaRepo.buscarPorId(parada.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoParada.PENDIENTE);
    }

    @Test
    void registrar_parada_fallida_actualiza_estado_y_motivo() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);

        RegistrarParadaRequest body = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.FALLIDA,
                null, null, null, MotivoNovedad.CLIENTE_AUSENTE, Instant.now());

        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", parada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        Parada persistida = paradaRepo.buscarPorId(parada.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoParada.FALLIDA);
        assertThat(persistida.getMotivoNovedad()).isEqualTo(MotivoNovedad.CLIENTE_AUSENTE);
    }

    @Test
    void registrar_parada_novedad_actualiza_estado_y_motivo() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);

        RegistrarParadaRequest body = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.NOVEDAD,
                null, null, null, MotivoNovedad.DAÑADO_EN_RUTA, Instant.now());

        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", parada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        Parada persistida = paradaRepo.buscarPorId(parada.getId()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoParada.NOVEDAD);
        assertThat(persistida.getMotivoNovedad()).isEqualTo(MotivoNovedad.DAÑADO_EN_RUTA);
    }

    // ── POST /paradas/{paradaId}/foto (multipart) ─────────────────────────────

    @Test
    void subir_foto_almacena_archivo_y_retorna_url() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);
        MockMultipartFile foto = new MockMultipartFile(
                "archivo", "evidencia.jpg", "image/jpeg",
                new byte[]{1, 2, 3, 4, 5});

        mockMvc.perform(multipart("/api/conductor/paradas/{paradaId}/foto", parada.getId())
                        .file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists());
    }

    @Test
    void subir_foto_para_parada_inexistente_devuelve_404() throws Exception {
        MockMultipartFile foto = new MockMultipartFile(
                "archivo", "evidencia.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/conductor/paradas/{paradaId}/foto", UUID.randomUUID())
                        .file(foto))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PARADA_NO_ENCONTRADA"));
    }

    @Test
    void subir_foto_vacia_devuelve_422() throws Exception {
        Ruta ruta = crearRutaEnTransito();
        Parada parada = crearParadaPendiente(ruta.getId(), 1);
        MockMultipartFile fotoVacia = new MockMultipartFile(
                "archivo", "vacia.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/conductor/paradas/{paradaId}/foto", parada.getId())
                        .file(fotoVacia))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── E2E: flujo completo CONFIRMADA → EN_TRANSITO → paradas → CERRADA ──────

    @Test
    void flujo_completo_conductor_de_extremo_a_extremo() throws Exception {
        // 1) Setup: ruta CONFIRMADA con 3 paradas pendientes
        Ruta ruta = crearRutaConfirmada();
        Parada p1 = crearParadaPendiente(ruta.getId(), 1);
        Parada p2 = crearParadaPendiente(ruta.getId(), 2);
        Parada p3 = crearParadaPendiente(ruta.getId(), 3);

        // 2) GET /ruta-activa retorna ruta CONFIRMADA con 3 paradas
        mockMvc.perform(get("/api/conductor/ruta-activa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.paradas.length()").value(3));

        // 3) POST /iniciar-transito → ruta EN_TRANSITO
        mockMvc.perform(post("/api/conductor/rutas/{id}/iniciar-transito", ruta.getId()))
                .andExpect(status().isNoContent());
        assertThat(rutaRepo.buscarPorId(ruta.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRuta.EN_TRANSITO);

        // 4) POST foto para p1 → URL retornada
        MockMultipartFile foto = new MockMultipartFile(
                "archivo", "pod.jpg", "image/jpeg", new byte[]{10, 20, 30});
        String fotoJson = mockMvc.perform(multipart("/api/conductor/paradas/{paradaId}/foto", p1.getId())
                        .file(foto))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fotoUrl = objectMapper.readTree(fotoJson).get("url").asText();
        assertThat(fotoUrl).isNotBlank();

        // 5) POST registrar EXITOSA para p1 con la URL recién subida
        RegistrarParadaRequest exitosa = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.EXITOSA,
                fotoUrl, null, "Carlos Receptor", null,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", p1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exitosa)))
                .andExpect(status().isNoContent());
        assertThat(paradaRepo.buscarPorId(p1.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoParada.EXITOSA);

        // 6) POST registrar FALLIDA para p2
        RegistrarParadaRequest fallida = new RegistrarParadaRequest(
                RegistrarParadaRequest.TipoResultado.FALLIDA,
                null, null, null, MotivoNovedad.CLIENTE_AUSENTE,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        mockMvc.perform(post("/api/conductor/paradas/{paradaId}/registrar", p2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fallida)))
                .andExpect(status().isNoContent());
        assertThat(paradaRepo.buscarPorId(p2.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoParada.FALLIDA);

        // 7) POST cerrar con confirmarConPendientes=false rechaza por p3 PENDIENTE
        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", ruta.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARADAS_PENDIENTES"));

        // 8) POST cerrar con confirmarConPendientes=true marca p3 SIN_GESTION y cierra ruta
        mockMvc.perform(post("/api/conductor/rutas/{id}/cerrar", ruta.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CierreRutaRequest(true))))
                .andExpect(status().isNoContent());

        Ruta cerrada = rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
        assertThat(cerrada.getEstado()).isEqualTo(EstadoRuta.CERRADA_MANUAL);
        assertThat(cerrada.getFechaHoraCierre()).isNotNull();

        Parada p3Final = paradaRepo.buscarPorId(p3.getId()).orElseThrow();
        assertThat(p3Final.getEstado()).isEqualTo(EstadoParada.SIN_GESTION_CONDUCTOR);
        assertThat(p3Final.getOrigen()).isEqualTo(OrigenParada.SISTEMA);

        Vehiculo vehiculo = vehiculoRepo.buscarPorId(vehiculoId).orElseThrow();
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(vehiculo.getConductorId()).isEqualTo(conductorId);

        Conductor conductor = conductorRepo.buscarPorId(conductorId).orElseThrow();
        assertThat(conductor.getEstado()).isEqualTo(EstadoConductor.ACTIVO);
    }
}
