package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.ActualizarVehiculoCommand;
import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.VehiculoEnTransitoException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActualizarVehiculoUseCaseTest {

    @Mock
    VehiculoRepositoryPort repo;

    ActualizarVehiculoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ActualizarVehiculoUseCase(repo);
    }

    //helpers
    private Vehiculo vehiculoDisponible() {
        return Vehiculo.nuevo(new RegistrarVehiculoCommand(
                "ABC123", TipoVehiculo.MOTO, "Yamaha NMX 155",
                BigDecimal.valueOf(40), BigDecimal.valueOf(0.3), "d2g"));
    }

    private Vehiculo vehiculoEnTransito() {
        Vehiculo v = vehiculoDisponible();
        v.asignarConductor(UUID.randomUUID());
        return v;
    }

    private ActualizarVehiculoCommand commandActualizacion() {
        return new ActualizarVehiculoCommand(
                TipoVehiculo.VAN, "Renault Kangoo",
                BigDecimal.valueOf(1200), BigDecimal.valueOf(4.5), "d2f");
    }

    @Test
    void actualizacion_exitosa_cuando_vehiculo_esta_disponible() {
        // Given: vehículo existente en estado DISPONIBLE (sin conductor asignado)
        UUID id = UUID.randomUUID();
        Vehiculo vehiculo = vehiculoDisponible();
        when(repo.buscarPorId(id)).thenReturn(Optional.of(vehiculo));
        when(repo.guardar(any())).thenReturn(vehiculo);

        // When: se solicita actualizar modelo, tipo y zona
        useCase.ejecutar(id, commandActualizacion());

        // Then: el repositorio persiste el vehículo modificado
        verify(repo, times(1)).guardar(vehiculo);
    }

    @Test
    void actualizacion_bloqueada_cuando_vehiculo_esta_en_transito() {
        // Given: vehículo con conductor asignado (estado EN_TRANSITO)
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.of(vehiculoEnTransito()));

        // When / Then: no se puede modificar un vehículo en ruta activa
        assertThatThrownBy(() -> useCase.ejecutar(id, commandActualizacion()))
                .isInstanceOf(VehiculoEnTransitoException.class);
        verify(repo, never()).guardar(any());
    }

    @Test
    void actualizacion_falla_cuando_vehiculo_no_existe() {
        // Given: el id no corresponde a ningún vehículo registrado
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then: se lanza VehiculoNoEncontradoException sin persistir nada
        assertThatThrownBy(() -> useCase.ejecutar(id, commandActualizacion()))
                .isInstanceOf(VehiculoNoEncontradoException.class);
        verify(repo, never()).guardar(any());
    }
}
