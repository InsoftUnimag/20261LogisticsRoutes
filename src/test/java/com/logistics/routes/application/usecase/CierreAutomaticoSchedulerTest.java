package com.logistics.routes.application.usecase;

import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.application.port.out.IntegracionModulo3Port;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
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
import com.logistics.routes.infrastructure.scheduler.CierreAutomaticoScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreAutomaticoSchedulerTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;
    @Mock ConductorRepositoryPort conductorRepository;
    @Mock VehiculoRepositoryPort vehiculoRepository;
    @Mock IntegracionModulo1Port integracionModulo1;
    @Mock IntegracionModulo3Port integracionModulo3;
    @Mock NotificacionDespachadorPort notificacion;

    CierreAutomaticoScheduler scheduler;

    UUID rutaId      = UUID.randomUUID();
    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        CerrarRutasExcedidasUseCase useCase = new CerrarRutasExcedidasUseCase(
                rutaRepository, paradaRepository, conductorRepository,
                vehiculoRepository, integracionModulo1, integracionModulo3, notificacion);
        scheduler = new CierreAutomaticoScheduler(useCase);
    }

    @Test
    void scheduler_cierra_rutas_excedidas_y_notifica_despachador() {
        Instant inicioHaceTreesDias = Instant.now().minus(3, ChronoUnit.DAYS);
        Ruta rutaExcedida = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 150.0,
                TipoVehiculo.NHR, vehiculoId, conductorId,
                Instant.now().minusSeconds(86400 * 4), Instant.now().minusSeconds(86400),
                inicioHaceTreesDias, null, null);

        Conductor conductor = Conductor.reconstituir(conductorId, "Luis Torres", "luis@test.com",
                ModeloContrato.RECORRIDO_COMPLETO, EstadoConductor.EN_RUTA, vehiculoId);
        Vehiculo vehiculo = Vehiculo.reconstituir(vehiculoId, "DEF456", TipoVehiculo.NHR, "Turbo",
                1000.0, 15.0, "d3gpz", EstadoVehiculo.EN_TRANSITO, conductorId);

        Parada exitosa = Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 1,
                "Calle 1", 4.71, -74.07, null, null, null,
                EstadoParada.EXITOSA, null, Instant.now().minusSeconds(3600),
                null, "foto.jpg", "Receptor", OrigenParada.CONDUCTOR);

        when(rutaRepository.buscarRutasEnTransitoExcedidas(any())).thenReturn(List.of(rutaExcedida));
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(rutaExcedida));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(exitosa));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));

        scheduler.ejecutarCierresAutomaticos();

        assertThat(rutaExcedida.getEstado()).isEqualTo(EstadoRuta.CERRADA_AUTOMATICA);
        assertThat(rutaExcedida.getTipoCierre()).isEqualTo(TipoCierre.AUTOMATICO);
        verify(integracionModulo3).publishRutaCerrada(any());
        verify(notificacion).notificarAlertaPrioritaria(any());
    }

    @Test
    void scheduler_no_falla_cuando_no_hay_rutas_excedidas() {
        when(rutaRepository.buscarRutasEnTransitoExcedidas(any())).thenReturn(List.of());

        scheduler.ejecutarCierresAutomaticos();

        verify(integracionModulo3, org.mockito.Mockito.never()).publishRutaCerrada(any());
        verify(notificacion, org.mockito.Mockito.never()).notificarAlertaPrioritaria(any());
    }
}
