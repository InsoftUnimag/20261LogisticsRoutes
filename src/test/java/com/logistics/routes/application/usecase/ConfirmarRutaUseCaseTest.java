package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.ConfirmarDespachoCommand;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmarRutaUseCaseTest {

    @Mock
    RutaRepositoryPort rutaRepository;
    @Mock
    VehiculoRepositoryPort vehiculoRepository;
    @Mock
    ConductorRepositoryPort conductorRepository;
    @Mock
    ParadaRepositoryPort paradaRepository;

    // Mocks de dominio para controlar getEstado()/getTipo() sin depender del
    // constructor real
    @Mock
    Conductor conductor;
    @Mock
    Vehiculo vehiculo;

    ConfirmarDespachoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmarDespachoUseCase(rutaRepository, vehiculoRepository, conductorRepository,
                paradaRepository);
    }

    @Test
    void confirma_ruta_en_lista_para_despacho_asignando_conductor_y_vehiculo() {
        // Given: ruta en LISTA_PARA_DESPACHO
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        ruta.transicionarAListaParaDespacho();

        UUID conductorId = UUID.randomUUID();
        UUID vehiculoId = UUID.randomUUID();

        // Conductor activo y vehículo disponible del tipo que requiere la ruta
        when(conductor.getEstado()).thenReturn(EstadoConductor.ACTIVO);
        when(conductor.getId()).thenReturn(conductorId);
        when(vehiculo.getEstado()).thenReturn(EstadoVehiculo.DISPONIBLE);
        when(vehiculo.getTipo()).thenReturn(ruta.getTipoVehiculoRequerido());
        when(vehiculo.getId()).thenReturn(vehiculoId);

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));
        when(paradaRepository.buscarPorRutaId(ruta.getId())).thenReturn(List.of());
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(ruta.getId(), new ConfirmarDespachoCommand(conductorId, vehiculoId));

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
        assertThatThrownBy(
                () -> useCase.ejecutar(rutaId, new ConfirmarDespachoCommand(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(RutaNoEncontradaException.class);

        verify(rutaRepository, never()).guardar(any());
    }

    @Test
    void lanza_excepcion_cuando_la_ruta_no_esta_en_lista_para_despacho() {
        // Given: ruta en estado CREADA (no LISTA_PARA_DESPACHO)
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        UUID conductorId = UUID.randomUUID();
        UUID vehiculoId = UUID.randomUUID();

        // Conductor y vehículo válidos — el error debe venir de la transición de estado
        // de la ruta
        when(conductor.getEstado()).thenReturn(EstadoConductor.ACTIVO);
        when(conductor.getId()).thenReturn(conductorId);
        when(vehiculo.getEstado()).thenReturn(EstadoVehiculo.DISPONIBLE);
        when(vehiculo.getTipo()).thenReturn(ruta.getTipoVehiculoRequerido());
        when(vehiculo.getId()).thenReturn(vehiculoId);

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));
        when(paradaRepository.buscarPorRutaId(ruta.getId())).thenReturn(List.of());

        // When / Then: confirmar desde CREADA debe lanzar IllegalStateException
        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductorId, vehiculoId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LISTA_PARA_DESPACHO");

        verify(rutaRepository, never()).guardar(any());
    }
}
