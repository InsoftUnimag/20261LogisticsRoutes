package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesarRutasVencidasUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock NotificacionDespachadorPort notificacion;

    ProcesarRutasVencidasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarRutasVencidasUseCase(rutaRepository, notificacion);
    }

    @Test
    void transiciona_y_notifica_cada_ruta_vencida() {
        // Given: 2 rutas CREADA con fecha límite en el pasado
        Ruta ruta1 = Ruta.nueva("d29ej", Instant.now().minus(1, ChronoUnit.DAYS));
        Ruta ruta2 = Ruta.nueva("d29ek", Instant.now().minus(2, ChronoUnit.DAYS));

        when(rutaRepository.buscarRutasVencidas(any(Instant.class)))
                .thenReturn(List.of(ruta1, ruta2));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar();

        // Then: ambas rutas quedan en LISTA_PARA_DESPACHO
        assertThat(ruta1.getEstado()).isEqualTo(EstadoRuta.LISTA_PARA_DESPACHO);
        assertThat(ruta2.getEstado()).isEqualTo(EstadoRuta.LISTA_PARA_DESPACHO);

        verify(rutaRepository, times(1)).guardar(ruta1);
        verify(rutaRepository, times(1)).guardar(ruta2);
        verify(notificacion, times(2)).notificarRutaListaParaDespacho(
                any(UUID.class), anyString(), anyDouble(), any(TipoVehiculo.class), eq("vencimiento_plazo"));
    }

    @Test
    void no_hace_nada_cuando_no_hay_rutas_vencidas() {
        // Given: el repositorio no tiene rutas vencidas
        when(rutaRepository.buscarRutasVencidas(any(Instant.class)))
                .thenReturn(List.of());

        // When
        useCase.ejecutar();

        // Then: no guarda ni notifica
        verify(rutaRepository, never()).guardar(any());
        verifyNoInteractions(notificacion);
    }

    @Test
    void notifica_con_motivo_vencimiento_plazo() {
        // Given: una ruta vencida
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().minus(1, ChronoUnit.DAYS));
        when(rutaRepository.buscarRutasVencidas(any(Instant.class)))
                .thenReturn(List.of(ruta));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar();

        // Then: el motivo propagado a la notificación es "vencimiento_plazo"
        verify(notificacion, times(1)).notificarRutaListaParaDespacho(
                eq(ruta.getId()),
                eq(ruta.getZona()),
                eq(ruta.getPesoAcumuladoKg()),
                eq(ruta.getTipoVehiculoRequerido()),
                eq("vencimiento_plazo"));
    }
}
