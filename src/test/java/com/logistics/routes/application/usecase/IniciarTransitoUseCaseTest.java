package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.IniciarTransitoCommand;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.RutaEstadoInvalidoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IniciarTransitoUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;
    @Mock IntegracionModulo1Port integracionModulo1;

    IniciarTransitoUseCase useCase;

    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();
    UUID rutaId      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new IniciarTransitoUseCase(rutaRepository, paradaRepository, integracionModulo1);
    }

    private Ruta rutaConfirmada() {
        return Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.CONFIRMADA, 120.0,
                TipoVehiculo.MOTO, vehiculoId, conductorId,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(7200),
                null, null, null);
    }

    @Test
    void inicio_exitoso_transiciona_a_en_transito_y_publica_eventos_por_parada_pendiente() {
        Ruta ruta = rutaConfirmada();
        Parada p1 = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 1,
                "Dir 1", 11.24, -74.21, null, null, null, EstadoParada.PENDIENTE, 
                null, null, null, null, null, OrigenParada.SISTEMA);
        Parada p2 = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 2,
                "Dir 2", 11.25, -74.20, null, null, null, EstadoParada.PENDIENTE, 
                null, null, null, null, null, OrigenParada.SISTEMA);

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(p1, p2));

        Ruta resultado = useCase.ejecutar(new IniciarTransitoCommand(rutaId, conductorId));

        assertThat(resultado.getEstado()).isEqualTo(EstadoRuta.EN_TRANSITO);
        assertThat(resultado.getFechaHoraInicio()).isNotNull();
        verify(integracionModulo1, times(2)).publishPaqueteEnTransito(any(), eq(rutaId), any());
    }

    @Test
    void no_publica_evento_para_paradas_no_pendientes() {
        Ruta ruta = rutaConfirmada();
        Parada pendiente = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 1,
                "Dir 1", 11.24, -74.21, null, null, null, EstadoParada.PENDIENTE, 
                null, null, null, null, null, OrigenParada.SISTEMA);
        Parada excluida = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 2,
                "Dir 2", 11.25, -74.20, null, null, null, EstadoParada.EXCLUIDA_DESPACHO, 
                null, null, null, null, null, OrigenParada.SISTEMA);

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(pendiente, excluida));

        useCase.ejecutar(new IniciarTransitoCommand(rutaId, conductorId));

        verify(integracionModulo1, times(1)).publishPaqueteEnTransito(any(), any(), any());
    }

    @Test
    void lanza_excepcion_cuando_ruta_no_existe() {
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(new IniciarTransitoCommand(rutaId, conductorId)))
                .isInstanceOf(RutaNoEncontradaException.class);
    }

    @Test
    void lanza_excepcion_cuando_ruta_no_esta_en_confirmada() {
        Ruta enTransito = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 120.0,
                TipoVehiculo.MOTO, vehiculoId, conductorId,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(7200),
                Instant.now().minusSeconds(1800), null, null);
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(enTransito));

        assertThatThrownBy(() -> useCase.ejecutar(new IniciarTransitoCommand(rutaId, conductorId)))
                .isInstanceOf(RutaEstadoInvalidoException.class);
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_es_el_asignado() {
        Ruta ruta = rutaConfirmada();
        UUID otroConductor = UUID.randomUUID();
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> useCase.ejecutar(new IniciarTransitoCommand(rutaId, otroConductor)))
                .isInstanceOf(ConductorNoDisponibleException.class);
    }
}
