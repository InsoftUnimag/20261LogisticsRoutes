package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarParadaCommand;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEnTransitoException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarParadaUseCaseTest {

    @Mock ParadaRepositoryPort paradaRepository;
    @Mock RutaRepositoryPort rutaRepository;
    @Mock IntegracionModulo1Port integracionModulo1;

    RegistrarParadaUseCase useCase;

    UUID paradaId  = UUID.randomUUID();
    UUID rutaId    = UUID.randomUUID();
    UUID paqueteId = UUID.randomUUID();
    Instant ahora  = Instant.now();

    @BeforeEach
    void setUp() {
        useCase = new RegistrarParadaUseCase(paradaRepository, rutaRepository, integracionModulo1);
    }

    private Parada paradaPendiente() {
        return Parada.reconstituir(paradaId, rutaId, paqueteId, 1,
                "Calle 10 #5-20", 11.24, -74.21,
                null, null, null,
                EstadoParada.PENDIENTE, null, null, null, null, null,
                OrigenParada.SISTEMA);
    }

    private Ruta rutaEnTransito() {
        return Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 80.0,
                TipoVehiculo.MOTO, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(3600), null, null);
    }

    // ── Exitosa ───────────────────────────────────────────────────────────────

    @Test
    void registrar_exitosa_actualiza_estado_y_publica_entregado() {
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.of(paradaPendiente()));
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(rutaEnTransito()));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarParadaCommand cmd = new RegistrarParadaCommand.Exitosa(
                paradaId, "http://foto.url", "http://firma.url", "Pedro Pérez", ahora);

        Parada resultado = useCase.ejecutar(cmd);

        assertThat(resultado.getEstado()).isEqualTo(EstadoParada.EXITOSA);
        assertThat(resultado.getFotoEvidenciaUrl()).isEqualTo("http://foto.url");
        assertThat(resultado.getNombreReceptor()).isEqualTo("Pedro Pérez");
        verify(integracionModulo1).publishPaqueteEntregado(eq(paqueteId), eq(rutaId), eq(ahora));
    }

    // ── Fallida ───────────────────────────────────────────────────────────────

    @Test
    void registrar_fallida_actualiza_estado_y_publica_fallida() {
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.of(paradaPendiente()));
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(rutaEnTransito()));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarParadaCommand cmd = new RegistrarParadaCommand.Fallida(
                paradaId, MotivoNovedad.CLIENTE_AUSENTE, ahora);

        Parada resultado = useCase.ejecutar(cmd);

        assertThat(resultado.getEstado()).isEqualTo(EstadoParada.FALLIDA);
        assertThat(resultado.getMotivoNovedad()).isEqualTo(MotivoNovedad.CLIENTE_AUSENTE);
        verify(integracionModulo1).publishParadaFallida(
                eq(paqueteId), eq(rutaId), eq("CLIENTE_AUSENTE"), eq(ahora));
    }

    // ── Novedad ───────────────────────────────────────────────────────────────

    @Test
    void registrar_novedad_actualiza_estado_y_publica_novedad_grave() {
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.of(paradaPendiente()));
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(rutaEnTransito()));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarParadaCommand cmd = new RegistrarParadaCommand.Novedad(
                paradaId, MotivoNovedad.DANIADO_EN_RUTA, ahora);

        Parada resultado = useCase.ejecutar(cmd);

        assertThat(resultado.getEstado()).isEqualTo(EstadoParada.NOVEDAD);
        assertThat(resultado.getMotivoNovedad()).isEqualTo(MotivoNovedad.DANIADO_EN_RUTA);
        verify(integracionModulo1).publishNovedadGrave(
                eq(paqueteId), eq(rutaId), eq("DANIADO_EN_RUTA"), eq(ahora));
    }

    // ── Errores ───────────────────────────────────────────────────────────────

    @Test
    void lanza_excepcion_cuando_parada_no_existe() {
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(
                new RegistrarParadaCommand.Fallida(paradaId, MotivoNovedad.CLIENTE_AUSENTE, ahora)))
                .isInstanceOf(ParadaNoEncontradaException.class);
    }

    @Test
    void lanza_excepcion_cuando_ruta_no_esta_en_transito() {
        Ruta confirmada = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.CONFIRMADA, 80.0,
                TipoVehiculo.MOTO, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().plusSeconds(3600),
                null, null, null);
        when(paradaRepository.buscarPorId(paradaId)).thenReturn(Optional.of(paradaPendiente()));
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> useCase.ejecutar(
                new RegistrarParadaCommand.Exitosa(paradaId, "url", null, "Receptor", ahora)))
                .isInstanceOf(RutaNoEnTransitoException.class);
    }
}
