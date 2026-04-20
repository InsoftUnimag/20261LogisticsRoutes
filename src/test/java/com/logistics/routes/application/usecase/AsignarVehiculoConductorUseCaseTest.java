package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;
import com.logistics.routes.domain.exception.VehiculoNoDisponibleException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.HistorialAsignacion;
import com.logistics.routes.domain.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignarVehiculoConductorUseCaseTest {

    @Mock ConductorRepositoryPort conductorRepo;
    @Mock VehiculoRepositoryPort vehiculoRepo;
    @Mock HistorialAsignacionRepositoryPort historialRepo;
    @Mock NotificacionDespachadorPort notificacion;

    AsignarVehiculoConductorUseCase useCase;

    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AsignarVehiculoConductorUseCase(
                conductorRepo, vehiculoRepo, historialRepo, notificacion);
    }

    private Conductor conductorActivo() {
        return Conductor.nuevo("Ana García", "ana@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO);
    }

    private Vehiculo vehiculoDisponible() {
        return Vehiculo.reconstituir(vehiculoId, "MNT478", TipoVehiculo.MOTO, "Yamaha",
                45, 0.45, "d2g", EstadoVehiculo.DISPONIBLE, null);
    }

    @Test
    void asignacion_exitosa_crea_historial_y_notifica() {
        // Given
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conductorActivo()));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculoDisponible()));
        when(conductorRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehiculoRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historialRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Conductor resultado = useCase.ejecutar(conductorId, vehiculoId);

        // Then
        assertThat(resultado.getVehiculoAsignadoId()).isEqualTo(vehiculoId);
        verify(historialRepo).guardar(any(HistorialAsignacion.class));
        verify(notificacion).notificarAsignacion(any(), eq(vehiculoId));
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_existe() {
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(conductorId, vehiculoId))
                .isInstanceOf(ConductorNoEncontradoException.class);
        verify(historialRepo, never()).guardar(any());
    }

    @Test
    void lanza_excepcion_cuando_vehiculo_no_existe() {
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conductorActivo()));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(conductorId, vehiculoId))
                .isInstanceOf(VehiculoNoEncontradoException.class);
    }

    @Test
    void lanza_excepcion_cuando_vehiculo_no_esta_disponible() {
        Vehiculo enTransito = Vehiculo.reconstituir(vehiculoId, "MNT478", TipoVehiculo.MOTO,
                "Yamaha", 45, 0.45, "d2g", EstadoVehiculo.EN_TRANSITO, UUID.randomUUID());
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conductorActivo()));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.of(enTransito));

        assertThatThrownBy(() -> useCase.ejecutar(conductorId, vehiculoId))
                .isInstanceOf(VehiculoNoDisponibleException.class);
    }

    @Test
    void lanza_excepcion_cuando_conductor_ya_tiene_vehiculo() {
        Conductor conVehiculo = Conductor.reconstituir(conductorId, "Ana García",
                "ana@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO,
                EstadoConductor.ACTIVO, UUID.randomUUID());
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conVehiculo));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculoDisponible()));

        assertThatThrownBy(() -> useCase.ejecutar(conductorId, vehiculoId))
                .isInstanceOf(ConductorYaAsignadoException.class);
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_esta_activo() {
        Conductor inactivo = Conductor.reconstituir(conductorId, "Ana García",
                "ana@logisticasm.com", ModeloContrato.RECORRIDO_COMPLETO,
                EstadoConductor.INACTIVO, null);
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(inactivo));
        when(vehiculoRepo.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculoDisponible()));

        assertThatThrownBy(() -> useCase.ejecutar(conductorId, vehiculoId))
                .isInstanceOf(ConductorNoDisponibleException.class);
    }
}
