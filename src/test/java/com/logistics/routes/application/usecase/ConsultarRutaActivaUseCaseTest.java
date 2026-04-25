package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.view.RutaActivaView;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoVehiculo;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarRutaActivaUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;

    ConsultarRutaActivaUseCase useCase;

    UUID conductorId = UUID.randomUUID();
    UUID rutaId      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ConsultarRutaActivaUseCase(rutaRepository, paradaRepository);
    }

    @Test
    void retorna_ruta_con_paradas_ordenadas_cuando_conductor_tiene_ruta_activa() {
        Ruta ruta = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 120.0,
                TipoVehiculo.MOTO, UUID.randomUUID(), conductorId,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(7200),
                Instant.now().minusSeconds(1800), null, null);

        Parada p1 = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 2,
                "Calle 10", 11.24, -74.21, null, null, null, EstadoParada.PENDIENTE, 
                null, null, null, null, null, OrigenParada.SISTEMA);
        Parada p2 = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 1,
                "Calle 5", 11.25, -74.20, null, null, null, EstadoParada.PENDIENTE, 
                null, null, null, null, null, OrigenParada.SISTEMA);

        when(rutaRepository.buscarRutaActivaPorConductorId(conductorId)).thenReturn(Optional.of(ruta));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(p1, p2));

        Optional<RutaActivaView> resultado = useCase.ejecutar(conductorId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().ruta().getId()).isEqualTo(rutaId);
        // paradas deben estar ordenadas por orden ascendente
        assertThat(resultado.get().paradas().get(0).getOrden()).isEqualTo(1);
        assertThat(resultado.get().paradas().get(1).getOrden()).isEqualTo(2);
    }

    @Test
    void retorna_empty_cuando_conductor_no_tiene_ruta_activa() {
        when(rutaRepository.buscarRutaActivaPorConductorId(conductorId)).thenReturn(Optional.empty());

        Optional<RutaActivaView> resultado = useCase.ejecutar(conductorId);

        assertThat(resultado).isEmpty();
    }
}
