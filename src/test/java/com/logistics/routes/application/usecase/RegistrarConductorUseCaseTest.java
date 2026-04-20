package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.RegistrarConductorCommand;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.model.Conductor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarConductorUseCaseTest {

    @Mock
    ConductorRepositoryPort repo;

    RegistrarConductorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarConductorUseCase(repo);
    }

    @Test
    void registro_exitoso_crea_conductor_activo_sin_vehiculo() {
        // Given
        RegistrarConductorCommand cmd = new RegistrarConductorCommand(
                "Carlos López", "carlos@logisticasm.com", ModeloContrato.POR_PARADA);
        Conductor esperado = Conductor.nuevo("Carlos López", "carlos@logisticasm.com", ModeloContrato.POR_PARADA);
        when(repo.existePorEmail("carlos@logisticasm.com")).thenReturn(false);
        when(repo.guardar(any())).thenReturn(esperado);

        // When
        Conductor resultado = useCase.ejecutar(cmd);

        // Then
        assertThat(resultado.getEstado()).isEqualTo(EstadoConductor.ACTIVO);
        assertThat(resultado.getVehiculoAsignadoId()).isNull();
        verify(repo, times(1)).guardar(any());
    }

    @Test
    void registro_bloqueado_cuando_email_ya_existe() {
        // Given
        when(repo.existePorEmail("carlos@logisticasm.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> useCase.ejecutar(new RegistrarConductorCommand(
                "Carlos López", "carlos@logisticasm.com", ModeloContrato.POR_PARADA)))
                .isInstanceOf(com.logistics.routes.domain.exception.EmailDuplicadoException.class);
        verify(repo, never()).guardar(any());
    }
}
