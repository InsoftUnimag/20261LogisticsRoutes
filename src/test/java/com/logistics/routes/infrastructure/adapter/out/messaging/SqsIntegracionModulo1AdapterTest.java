package com.logistics.routes.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.routes.application.event.NovedadGraveEvent;
import com.logistics.routes.application.event.PaqueteEnTransitoEvent;
import com.logistics.routes.application.event.PaqueteEntregadoEvent;
import com.logistics.routes.application.event.PaqueteExcluidoDespachoEvent;
import com.logistics.routes.application.event.ParadaFallidaEvent;
import com.logistics.routes.application.event.ParadasSinGestionarEvent;
import com.logistics.routes.domain.enums.TipoCierre;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null") // captor.capture() es @Nullable según el tipo pero nunca null en contexto Mockito
@ExtendWith(MockitoExtension.class)
class SqsIntegracionModulo1AdapterTest {

    private static final String QUEUE = "eventos-paquete-queue";

    @Mock SqsTemplate sqsTemplate;
    SqsIntegracionModulo1Adapter adapter;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new SqsIntegracionModulo1Adapter(sqsTemplate, objectMapper, QUEUE);
    }

    @Test
    void publishPaqueteEnTransito_envia_evento_a_la_cola_correcta() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T10:00:00Z");

        adapter.publishPaqueteEnTransito(paqueteId, rutaId, fecha);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteEnTransitoEvent ev = objectMapper.readValue(captor.getValue(), PaqueteEnTransitoEvent.class);
        assertThat(ev.tipoEvento()).isEqualTo("PAQUETE_EN_TRANSITO");
        assertThat(ev.paqueteId()).isEqualTo(paqueteId);
        assertThat(ev.rutaId()).isEqualTo(rutaId);
        assertThat(ev.fechaHoraEvento()).isEqualTo(fecha);
    }

    @Test
    void publishPaqueteEntregado_envia_evento_con_evidencia() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.now();

        adapter.publishPaqueteEntregado(paqueteId, rutaId, fecha,
                "https://s3/foto.jpg", "https://s3/firma.png");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteEntregadoEvent ev = objectMapper.readValue(captor.getValue(), PaqueteEntregadoEvent.class);
        assertThat(ev.tipoEvento()).isEqualTo("PAQUETE_ENTREGADO");
        assertThat(ev.evidencia().urlFoto()).isEqualTo("https://s3/foto.jpg");
        assertThat(ev.evidencia().urlFirma()).isEqualTo("https://s3/firma.png");
    }

    @Test
    void publishParadaFallida_envia_evento_con_motivo() throws Exception {
        adapter.publishParadaFallida(UUID.randomUUID(), UUID.randomUUID(),
                "CLIENTE_AUSENTE", Instant.now());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        ParadaFallidaEvent ev = objectMapper.readValue(captor.getValue(), ParadaFallidaEvent.class);
        assertThat(ev.motivo()).isEqualTo("CLIENTE_AUSENTE");
    }

    @Test
    void publishNovedadGrave_envia_evento_con_tipo_novedad() throws Exception {
        adapter.publishNovedadGrave(UUID.randomUUID(), UUID.randomUUID(),
                "DANIADO_EN_RUTA", Instant.now());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        NovedadGraveEvent ev = objectMapper.readValue(captor.getValue(), NovedadGraveEvent.class);
        assertThat(ev.tipoNovedad()).isEqualTo("DANIADO_EN_RUTA");
    }

    @Test
    void publishParadasSinGestionar_envia_evento_con_lista_de_paquetes() throws Exception {
        UUID rutaId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        adapter.publishParadasSinGestionar(rutaId, TipoCierre.AUTOMATICO, List.of(p1, p2));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        ParadasSinGestionarEvent ev = objectMapper.readValue(captor.getValue(), ParadasSinGestionarEvent.class);
        assertThat(ev.tipoCierre()).isEqualTo("AUTOMATICO");
        assertThat(ev.paquetes()).extracting("paqueteId").containsExactly(p1, p2);
    }

    @Test
    void publishPaqueteExcluidoDespacho_envia_evento_con_ids() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();

        adapter.publishPaqueteExcluidoDespacho(paqueteId, rutaId, "motivo libre", Instant.now());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteExcluidoDespachoEvent ev = objectMapper.readValue(captor.getValue(), PaqueteExcluidoDespachoEvent.class);
        assertThat(ev.paqueteId()).isEqualTo(paqueteId);
        assertThat(ev.rutaId()).isEqualTo(rutaId);
    }

    @Test
    void no_envia_a_otra_cola_diferente_de_la_configurada() {
        adapter.publishPaqueteEnTransito(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        verify(sqsTemplate).send(eq(QUEUE), any(String.class));
    }
}
