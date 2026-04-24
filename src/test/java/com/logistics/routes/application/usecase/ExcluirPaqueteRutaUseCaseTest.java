package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.model.Parada;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcluirPaqueteRutaUseCaseTest {

    @Mock ParadaRepositoryPort paradaRepository;
    @Mock IntegracionModulo1Port integracionM1;

    ExcluirPaqueteRutaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExcluirPaqueteRutaUseCase(paradaRepository, integracionM1);
    }

    @Test
    void marca_parada_como_excluida_despacho_y_publica_evento_a_modulo1() {
        // Given: parada PENDIENTE para un paquete en la ruta
        UUID rutaId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();
        Parada parada = Parada.nueva(rutaId, paqueteId, "Calle 10 #20-30",
                11.240, -74.199, "ESTANDAR", "PREPAGO", null);

        when(paradaRepository.buscarPorRutaYPaquete(rutaId, paqueteId))
                .thenReturn(Optional.of(parada));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(rutaId, paqueteId, "paquete dañado en centro de acopio");

        // Then
        assertThat(parada.getEstado()).isEqualTo(EstadoParada.EXCLUIDA_DESPACHO);
        verify(paradaRepository).guardar(parada);
        verify(integracionM1).publishPaqueteExcluidoDespacho(
                eq(paqueteId), eq(rutaId), eq("paquete dañado en centro de acopio"), any(Instant.class));
    }

    @Test
    void lanza_excepcion_cuando_la_parada_no_existe() {
        UUID rutaId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();
        when(paradaRepository.buscarPorRutaYPaquete(rutaId, paqueteId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(rutaId, paqueteId, "motivo"))
                .isInstanceOf(ParadaNoEncontradaException.class);

        verify(paradaRepository, never()).guardar(any());
        verifyNoInteractions(integracionM1);
    }
}
