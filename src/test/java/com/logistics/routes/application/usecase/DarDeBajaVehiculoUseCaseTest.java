package com.logistics.routes.application.usecase;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DarDeBajaVehiculoUseCaseTest {

    @Mock
    VehiculoRepositoryPort repo;

    DarDeBajaVehiculoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DarDeBajaVehiculoUseCase(repo);
    }

    //helpers
    private Vehiculo vehiculoDisponible() {
        return Vehiculo.nuevo(new RegistrarVehiculoCommand(
                "XYZ999", TipoVehiculo.NHR, "Chevrolet NHR",
                BigDecimal.valueOf(3000), BigDecimal.valueOf(15), "d2g"));
    }

    private Vehiculo vehiculoEnTransito() {
        Vehiculo v = vehiculoDisponible();
        v.asignarConductor(UUID.randomUUID());
        return v;
    }

    @Test
    void baja_exitosa_cuando_vehiculo_disponible() {
        // Given: vehículo existente en estado DISPONIBLE
        UUID id = UUID.randomUUID();
        Vehiculo vehiculo = vehiculoDisponible();
        when(repo.buscarPorId(id)).thenReturn(Optional.of(vehiculo));
        when(repo.guardar(any())).thenReturn(vehiculo);

        // When: se solicita dar de baja (soft delete)
        useCase.ejecutar(id);

        // Then: el vehículo se guarda con estado INACTIVO
        ArgumentCaptor<Vehiculo> captor = ArgumentCaptor.forClass(Vehiculo.class);
        verify(repo).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoVehiculo.INACTIVO);
    }

    @Test
    void baja_bloqueada_cuando_vehiculo_en_transito() {
        // Given: vehículo con conductor asignado (no se puede dar de baja en ruta)
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.of(vehiculoEnTransito()));

        // When / Then: la baja lanza VehiculoEnTransitoException sin persistir
        assertThatThrownBy(() -> useCase.ejecutar(id))
                .isInstanceOf(VehiculoEnTransitoException.class);
        verify(repo, never()).guardar(any());
    }

    @Test
    void baja_falla_cuando_vehiculo_no_existe() {
        // Given: el id no corresponde a ningún vehículo registrado
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then: se lanza VehiculoNoEncontradoException sin persistir nada
        assertThatThrownBy(() -> useCase.ejecutar(id))
                .isInstanceOf(VehiculoNoEncontradoException.class);
        verify(repo, never()).guardar(any());
    }
}
