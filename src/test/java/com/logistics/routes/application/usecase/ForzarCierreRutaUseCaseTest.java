package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.enums.TipoCierre;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForzarCierreRutaUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;
    @Mock ConductorRepositoryPort conductorRepository;
    @Mock VehiculoRepositoryPort vehiculoRepository;
    @Mock IntegracionModulo1Port integracionModulo1;
    @Mock IntegracionModulo3Port integracionModulo3;

    ForzarCierreRutaUseCase useCase;

    UUID rutaId      = UUID.randomUUID();
    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ForzarCierreRutaUseCase(
                rutaRepository, paradaRepository, conductorRepository,
                vehiculoRepository, integracionModulo1, integracionModulo3);
    }

    @Test
    void cierre_forzado_transiciona_a_cerrada_forzada_e_ignora_pendientes() {
        Ruta ruta = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 200.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minusSeconds(7200), Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(3600), null, null);

        UUID paqueteId = UUID.randomUUID();
        Parada pendiente = Parada.reconstituir(UUID.randomUUID(), rutaId, paqueteId, 1,
                "Calle 5 #10-20", 4.71, -74.07, null, null, null,
                EstadoParada.PENDIENTE, null, null, null, null, null, OrigenParada.SISTEMA);

        Conductor conductor = Conductor.reconstituir(conductorId, "Juan Pérez", "juan@test.com",
                ModeloContrato.RECORRIDO_COMPLETO, EstadoConductor.EN_RUTA, vehiculoId);
        Vehiculo vehiculo = Vehiculo.reconstituir(vehiculoId, "XYZ789", TipoVehiculo.VAN, "Transit",
                300.0, 5.0, "d3gpz", EstadoVehiculo.EN_TRANSITO, conductorId);

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(pendiente));
        when(paradaRepository.guardarTodas(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));

        useCase.ejecutar(rutaId);

        assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.CERRADA_FORZADA);
        assertThat(ruta.getTipoCierre()).isEqualTo(TipoCierre.FORZADO_DESPACHADOR);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoParada.SIN_GESTION_CONDUCTOR);
        assertThat(conductor.getEstado()).isEqualTo(EstadoConductor.ACTIVO);
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);

        verify(integracionModulo1).publishParadasSinGestionar(any(), any(), any());
        verify(integracionModulo3).publishRutaCerrada(any());
    }
}
