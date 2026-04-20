package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;
import com.logistics.routes.domain.model.Conductor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DarDeBajaConductorUseCaseTest {

    @Mock
    ConductorRepositoryPort repo;

    DarDeBajaConductorUseCase useCase;

    UUID conductorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DarDeBajaConductorUseCase(repo);
    }

    @Test
    void baja_exitosa_de_conductor_sin_vehiculo() {
        // Given: conductor activo sin vehículo
        Conductor conductor = Conductor.reconstituir(conductorId, "Juan Perez",
                "juan@logisticasm.com", ModeloContrato.POR_PARADA, EstadoConductor.ACTIVO, null);
        when(repo.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(repo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(conductorId);

        // Then: se persiste en estado INACTIVO
        verify(repo).guardar(argThat(c -> c.getEstado() == EstadoConductor.INACTIVO));
    }

    @Test
    void lanza_excepcion_cuando_conductor_tiene_vehiculo_asignado() {
        // Given: conductor con vehículo — no puede darse de baja
        Conductor conductor = Conductor.reconstituir(conductorId, "Juan Perez",
                "juan@logisticasm.com", ModeloContrato.POR_PARADA,
                EstadoConductor.ACTIVO, UUID.randomUUID());
        when(repo.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));

        assertThatThrownBy(() -> useCase.ejecutar(conductorId))
                .isInstanceOf(ConductorYaAsignadoException.class);
        verify(repo, never()).guardar(any());
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_existe() {
        when(repo.buscarPorId(conductorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(conductorId))
                .isInstanceOf(ConductorNoEncontradoException.class);
    }
}
