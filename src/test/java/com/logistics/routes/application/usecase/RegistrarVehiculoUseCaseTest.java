package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.PlacaDuplicadaException;
import com.logistics.routes.domain.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarVehiculoUseCaseTest {

    @Mock
    VehiculoRepositoryPort repo;

    RegistrarVehiculoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarVehiculoUseCase(repo);
    }

    //helper
    private RegistrarVehiculoCommand commandValido(String placa) {
        return new RegistrarVehiculoCommand(
                placa, TipoVehiculo.MOTO, "Yamaha NMX 155",
                BigDecimal.valueOf(40), BigDecimal.valueOf(0.3), "d2g");
    }

    @Test
    void registro_exitoso_guarda_vehiculo_en_estado_disponible() {
        // Given: placa libre + repositorio listo para guardar
        RegistrarVehiculoCommand cmd = commandValido("ABC123");
        Vehiculo esperado = Vehiculo.nuevo(cmd);
        when(repo.existePorPlaca("ABC123")).thenReturn(false);
        when(repo.guardar(any())).thenReturn(esperado);

        // When: se ejecuta el caso de uso
        Vehiculo resultado = useCase.ejecutar(cmd);

        // Then: el vehículo queda en estado DISPONIBLE y se persiste una vez
        assertThat(resultado.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(resultado.getPlaca()).isEqualTo("ABC123");
        verify(repo, times(1)).guardar(any());
    }

    @Test
    void registro_bloqueado_cuando_placa_ya_existe() {
        // Given: la placa ya está registrada en el sistema
        when(repo.existePorPlaca("ABC123")).thenReturn(true);

        // When / Then: el caso de uso lanza PlacaDuplicadaException sin llamar a guardar
        assertThatThrownBy(() -> useCase.ejecutar(commandValido("ABC123")))
                .isInstanceOf(PlacaDuplicadaException.class);
        verify(repo, never()).guardar(any());
    }
}
