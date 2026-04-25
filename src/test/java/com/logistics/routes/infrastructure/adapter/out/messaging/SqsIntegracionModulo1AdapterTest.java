package com.logistics.routes.infrastructure.adapter.out.messaging;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsIntegracionModulo1AdapterTest {

    private static final String QUEUE = "eventos-paquete-queue";

    @Mock SqsTemplate sqsTemplate;
    SqsIntegracionModulo1Adapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SqsIntegracionModulo1Adapter(sqsTemplate);
        ReflectionTestUtils.setField(adapter, "eventosPaqueteQueue", QUEUE);
    }

    @Test
    void publishPaqueteEnTransito_envia_evento_a_la_cola_correcta() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-04-24T10:00:00Z");

        adapter.publishPaqueteEnTransito(paqueteId, rutaId, fecha);

        ArgumentCaptor<PaqueteEnTransitoEvent> captor = ArgumentCaptor.forClass(PaqueteEnTransitoEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteEnTransitoEvent ev = captor.getValue();
        assertThat(ev.tipoEvento()).isEqualTo("PAQUETE_EN_TRANSITO");
        assertThat(ev.paqueteId()).isEqualTo(paqueteId);
        assertThat(ev.rutaId()).isEqualTo(rutaId);
        assertThat(ev.fechaHoraEvento()).isEqualTo(fecha);
    }

    @Test
    void publishPaqueteEntregado_envia_evento_con_evidencia() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        Instant fecha = Instant.now();

        adapter.publishPaqueteEntregado(paqueteId, rutaId, fecha,
                "https://s3/foto.jpg", "https://s3/firma.png");

        ArgumentCaptor<PaqueteEntregadoEvent> captor = ArgumentCaptor.forClass(PaqueteEntregadoEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteEntregadoEvent ev = captor.getValue();
        assertThat(ev.tipoEvento()).isEqualTo("PAQUETE_ENTREGADO");
        assertThat(ev.evidencia().urlFoto()).isEqualTo("https://s3/foto.jpg");
        assertThat(ev.evidencia().urlFirma()).isEqualTo("https://s3/firma.png");
    }

    @Test
    void publishParadaFallida_envia_evento_con_motivo() {
        adapter.publishParadaFallida(UUID.randomUUID(), UUID.randomUUID(),
                "CLIENTE_AUSENTE", Instant.now());

        ArgumentCaptor<ParadaFallidaEvent> captor = ArgumentCaptor.forClass(ParadaFallidaEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        assertThat(captor.getValue().motivo()).isEqualTo("CLIENTE_AUSENTE");
    }

    @Test
    void publishNovedadGrave_envia_evento_con_tipo_novedad() {
        adapter.publishNovedadGrave(UUID.randomUUID(), UUID.randomUUID(),
                "DAÑADO_EN_RUTA", Instant.now());

        ArgumentCaptor<NovedadGraveEvent> captor = ArgumentCaptor.forClass(NovedadGraveEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        assertThat(captor.getValue().tipoNovedad()).isEqualTo("DAÑADO_EN_RUTA");
    }

    @Test
    void publishParadasSinGestionar_envia_evento_con_lista_de_paquetes() {
        UUID rutaId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        adapter.publishParadasSinGestionar(rutaId, TipoCierre.AUTOMATICO, List.of(p1, p2));

        ArgumentCaptor<ParadasSinGestionarEvent> captor = ArgumentCaptor.forClass(ParadasSinGestionarEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        ParadasSinGestionarEvent ev = captor.getValue();
        assertThat(ev.tipoCierre()).isEqualTo("AUTOMATICO");
        assertThat(ev.paquetes()).extracting("paqueteId").containsExactly(p1, p2);
    }

    @Test
    void publishPaqueteExcluidoDespacho_envia_evento_con_ids() {
        UUID paqueteId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();

        adapter.publishPaqueteExcluidoDespacho(paqueteId, rutaId, "motivo libre", Instant.now());

        ArgumentCaptor<PaqueteExcluidoDespachoEvent> captor = ArgumentCaptor.forClass(PaqueteExcluidoDespachoEvent.class);
        verify(sqsTemplate).send(eq(QUEUE), captor.capture());
        PaqueteExcluidoDespachoEvent ev = captor.getValue();
        assertThat(ev.paqueteId()).isEqualTo(paqueteId);
        assertThat(ev.rutaId()).isEqualTo(rutaId);
    }

    @Test
    void no_envia_a_otra_cola_diferente_de_la_configurada() {
        adapter.publishPaqueteEnTransito(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        verify(sqsTemplate).send(eq(QUEUE), any(PaqueteEnTransitoEvent.class));
    }
}
