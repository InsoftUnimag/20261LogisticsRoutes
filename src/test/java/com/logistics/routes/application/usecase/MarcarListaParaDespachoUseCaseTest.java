package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.exception.RutaEstadoInvalidoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarcarListaParaDespachoUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock NotificacionDespachadorPort notificacion;

    MarcarListaParaDespachoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MarcarListaParaDespachoUseCase(rutaRepository, notificacion);
    }

    @Test
    void transiciona_ruta_creada_a_lista_para_despacho_y_notifica() {
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(1, ChronoUnit.DAYS));
        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Ruta resultado = useCase.ejecutar(ruta.getId());

        assertThat(resultado.getEstado()).isEqualTo(EstadoRuta.LISTA_PARA_DESPACHO);
        assertThat(resultado.getMotivoDespacho()).isEqualTo("dispatcher_manual");
        verify(rutaRepository).guardar(ruta);
        verify(notificacion).notificarRutaListaParaDespacho(
                eq(ruta.getId()),
                eq(ruta.getZona()),
                eq(ruta.getPesoAcumuladoKg()),
                eq(ruta.getTipoVehiculoRequerido()),
                eq("dispatcher_manual"));
    }

    @Test
    void lanza_RutaNoEncontradaException_cuando_la_ruta_no_existe() {
        UUID rutaId = UUID.randomUUID();
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(rutaId))
                .isInstanceOf(RutaNoEncontradaException.class);

        verify(rutaRepository, never()).guardar(any());
        verifyNoInteractions(notificacion);
    }

    @Test
    void lanza_RutaEstadoInvalidoException_cuando_la_ruta_no_esta_CREADA() {
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(1, ChronoUnit.DAYS));
        ruta.transicionarAListaParaDespacho("vencimiento_plazo");
        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId()))
                .isInstanceOf(RutaEstadoInvalidoException.class);

        verify(rutaRepository, never()).guardar(any());
        verifyNoInteractions(notificacion);
    }
}
