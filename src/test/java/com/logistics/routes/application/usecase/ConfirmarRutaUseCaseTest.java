package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
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
class ConfirmarRutaUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;

    ConfirmarRutaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmarRutaUseCase(rutaRepository);
    }

    @Test
    void confirma_ruta_en_lista_para_despacho_asignando_conductor_y_vehiculo() {
        // Given: ruta en LISTA_PARA_DESPACHO
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        ruta.transicionarAListaParaDespacho();

        UUID conductorId = UUID.randomUUID();
        UUID vehiculoId = UUID.randomUUID();

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(ruta.getId(), conductorId, vehiculoId);

        // Then: ruta queda en CONFIRMADA con conductor y vehículo asignados
        assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.CONFIRMADA);
        assertThat(ruta.getConductorId()).isEqualTo(conductorId);
        assertThat(ruta.getVehiculoId()).isEqualTo(vehiculoId);
        verify(rutaRepository, times(1)).guardar(ruta);
    }

    @Test
    void lanza_excepcion_cuando_la_ruta_no_existe() {
        // Given: ruta inexistente
        UUID rutaId = UUID.randomUUID();
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.ejecutar(rutaId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RutaNoEncontradaException.class);

        verify(rutaRepository, never()).guardar(any());
    }

    @Test
    void lanza_excepcion_cuando_la_ruta_no_esta_en_lista_para_despacho() {
        // Given: ruta en estado CREADA (no LISTA_PARA_DESPACHO)
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));

        // When / Then: confirmar desde CREADA debe lanzar IllegalStateException
        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LISTA_PARA_DESPACHO");

        verify(rutaRepository, never()).guardar(any());
    }
}
