package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.HistorialAsignacion;
import com.logistics.routes.domain.model.Vehiculo;
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
class DesvincularVehiculoConductorUseCaseTest {

    @Mock ConductorRepositoryPort conductorRepo;
    @Mock VehiculoRepositoryPort vehiculoRepo;
    @Mock HistorialAsignacionRepositoryPort historialRepo;
    @Mock NotificacionDespachadorPort notificacion;

    DesvincularVehiculoConductorUseCase useCase;

    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DesvincularVehiculoConductorUseCase(
                conductorRepo, vehiculoRepo, historialRepo, notificacion);
    }

    @Test
    void desvinculacion_exitosa_cierra_historial_y_notifica() {
        // Given: conductor con vehículo asignado
        Conductor conductor = Conductor.reconstituir(conductorId, "Ana García",
                "ana@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO,
                EstadoConductor.ACTIVO, vehiculoId);
        Vehiculo vehiculo = Vehiculo.reconstituir(vehiculoId, "MNT478", TipoVehiculo.MOTO,
                "Yamaha", 45, 0.45, "d2g", EstadoVehiculo.EN_TRANSITO, conductorId);
        HistorialAsignacion historialActivo = HistorialAsignacion.reconstituir(
                UUID.randomUUID(), conductorId, vehiculoId, Instant.now().minusSeconds(3600), null);

        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));
        when(historialRepo.buscarActivoPorConductorId(conductorId)).thenReturn(Optional.of(historialActivo));
        when(conductorRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehiculoRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Conductor resultado = useCase.ejecutar(conductorId);

        // Then: conductor sin vehículo, historial cerrado, notificación enviada
        assertThat(resultado.getVehiculoAsignadoId()).isNull();
        verify(historialRepo).guardar(argThat(h -> h.getFechaFin() != null));
        verify(notificacion).notificarDesvinculacion(conductorId, vehiculoId);
    }

    @Test
    void desvinculacion_de_conductor_sin_vehiculo_no_falla() {
        // Given: conductor sin vehículo asignado
        Conductor sinVehiculo = Conductor.reconstituir(conductorId, "Ana García",
                "ana@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO,
                EstadoConductor.ACTIVO, null);
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(sinVehiculo));
        when(conductorRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialRepo.buscarActivoPorConductorId(conductorId)).thenReturn(Optional.empty());

        // When
        Conductor resultado = useCase.ejecutar(conductorId);

        // Then: no falla, no notifica (no había asignación)
        assertThat(resultado.getVehiculoAsignadoId()).isNull();
        verify(notificacion, never()).notificarDesvinculacion(any(), any());
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_existe() {
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(conductorId))
                .isInstanceOf(ConductorNoEncontradoException.class);
    }
}
