package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.HistorialAsignacionRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.HistorialAsignacion;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarHistorialConductorUseCaseTest {

    @Mock ConductorRepositoryPort conductorRepo;
    @Mock HistorialAsignacionRepositoryPort historialRepo;

    ConsultarHistorialConductorUseCase useCase;

    UUID conductorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ConsultarHistorialConductorUseCase(conductorRepo, historialRepo);
    }

    @Test
    void retorna_historial_del_conductor() {
        // Given
        Conductor conductor = Conductor.reconstituir(conductorId, "Ana García",
                "ana@logisticasm.com", ModeloContrato.POR_PARADA, EstadoConductor.ACTIVO, null);
        HistorialAsignacion entrada = HistorialAsignacion.reconstituir(
                UUID.randomUUID(), conductorId, UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(historialRepo.buscarPorConductorId(conductorId)).thenReturn(List.of(entrada));

        // When
        List<HistorialAsignacion> resultado = useCase.ejecutar(conductorId);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getFechaFin()).isNotNull();
    }

    @Test
    void lanza_excepcion_cuando_conductor_no_existe() {
        when(conductorRepo.buscarPorId(conductorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(conductorId))
                .isInstanceOf(ConductorNoEncontradoException.class);
    }
}
