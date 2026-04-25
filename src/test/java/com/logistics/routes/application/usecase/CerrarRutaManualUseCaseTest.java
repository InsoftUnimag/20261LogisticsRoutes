package com.logistics.routes.application.usecase;

import com.logistics.routes.application.event.RutaCerradaEvent;
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
import com.logistics.routes.domain.exception.ParadasPendientesException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEnTransitoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CerrarRutaManualUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;
    @Mock ConductorRepositoryPort conductorRepository;
    @Mock VehiculoRepositoryPort vehiculoRepository;
    @Mock IntegracionModulo1Port integracionModulo1;
    @Mock IntegracionModulo3Port integracionModulo3;

    CerrarRutaManualUseCase useCase;

    UUID rutaId      = UUID.randomUUID();
    UUID conductorId = UUID.randomUUID();
    UUID vehiculoId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CerrarRutaManualUseCase(
                rutaRepository, paradaRepository, conductorRepository,
                vehiculoRepository, integracionModulo1, integracionModulo3);
    }

    private Ruta rutaEnTransito() {
        return Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.EN_TRANSITO, 120.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minusSeconds(7200), Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(3600), null, null);
    }

    private Conductor conductorEnRuta() {
        return Conductor.reconstituir(conductorId, "Ana López", "ana@test.com",
                ModeloContrato.RECORRIDO_COMPLETO, EstadoConductor.EN_RUTA, vehiculoId);
    }

    private Vehiculo vehiculoEnTransito() {
        return Vehiculo.reconstituir(vehiculoId, "ABC123", TipoVehiculo.VAN, "Transit",
                300.0, 5.0, "d3gpz", EstadoVehiculo.EN_TRANSITO, conductorId);
    }

    private Parada paradaPendiente(UUID paqueteId) {
        return Parada.reconstituir(UUID.randomUUID(), rutaId, paqueteId, 1,
                "Calle 10 #5-20", 4.71, -74.07, null, null, null,
                EstadoParada.PENDIENTE, null, null, null, null, null, OrigenParada.SISTEMA);
    }

    private Parada paradaExitosa() {
        return Parada.reconstituir(UUID.randomUUID(), rutaId, UUID.randomUUID(), 2,
                "Calle 11 #6-30", 4.72, -74.08, null, null, null,
                EstadoParada.EXITOSA, null, Instant.now().minusSeconds(600),
                "firma.jpg", "foto.jpg", "Carlos", OrigenParada.CONDUCTOR);
    }

    // ── Escenario 1: cierre exitoso sin paradas pendientes ───────────────────

    @Test
    void cierre_exitoso_sin_pendientes_transiciona_a_cerrada_manual() {
        Ruta ruta = rutaEnTransito();
        Conductor conductor = conductorEnRuta();
        Vehiculo vehiculo = vehiculoEnTransito();

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(paradaExitosa()));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));

        useCase.ejecutar(rutaId, false);

        assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.CERRADA_MANUAL);
        assertThat(ruta.getTipoCierre()).isEqualTo(TipoCierre.MANUAL);
        assertThat(ruta.getFechaHoraCierre()).isNotNull();
        assertThat(conductor.getEstado()).isEqualTo(EstadoConductor.ACTIVO);
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(vehiculo.getConductorId()).isEqualTo(conductorId);

        verify(conductorRepository).guardar(conductor);
        verify(vehiculoRepository).guardar(vehiculo);
        verify(integracionModulo3).publishRutaCerrada(any());
        verify(integracionModulo1, never()).publishParadasSinGestionar(any(), any(), any());
    }

    // ── Escenario 2: paradas pendientes y confirmarConPendientes=false ────────

    @Test
    void lanza_excepcion_cuando_hay_pendientes_y_no_se_confirma() {
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(rutaEnTransito()));
        when(paradaRepository.buscarPorRutaId(rutaId))
                .thenReturn(List.of(paradaPendiente(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.ejecutar(rutaId, false))
                .isInstanceOf(ParadasPendientesException.class);

        verify(rutaRepository, never()).guardar(any());
        verify(integracionModulo3, never()).publishRutaCerrada(any());
    }

    // ── Escenario 3: paradas pendientes y confirmarConPendientes=true ─────────

    @Test
    void cierre_con_pendientes_confirmadas_marca_sin_gestion_y_publica_eventos() {
        Ruta ruta = rutaEnTransito();
        Conductor conductor = conductorEnRuta();
        Vehiculo vehiculo = vehiculoEnTransito();
        UUID paqueteId1 = UUID.randomUUID();
        UUID paqueteId2 = UUID.randomUUID();
        Parada p1 = paradaPendiente(paqueteId1);
        Parada p2 = paradaPendiente(paqueteId2);

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(p1, p2));
        when(paradaRepository.guardarTodas(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));

        useCase.ejecutar(rutaId, true);

        assertThat(p1.getEstado()).isEqualTo(EstadoParada.SIN_GESTION_CONDUCTOR);
        assertThat(p2.getEstado()).isEqualTo(EstadoParada.SIN_GESTION_CONDUCTOR);
        assertThat(p1.getOrigen()).isEqualTo(OrigenParada.SISTEMA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> paquetesCaptor = ArgumentCaptor.forClass(List.class);
        verify(integracionModulo1).publishParadasSinGestionar(
                any(), any(), paquetesCaptor.capture());
        assertThat(paquetesCaptor.getValue()).containsExactlyInAnyOrder(paqueteId1, paqueteId2);

        assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.CERRADA_MANUAL);
        verify(integracionModulo3).publishRutaCerrada(any());
    }

    // ── Escenario 4: ruta no encontrada ───────────────────────────────────────

    @Test
    void lanza_excepcion_cuando_ruta_no_existe() {
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(rutaId, false))
                .isInstanceOf(RutaNoEncontradaException.class);
    }

    // ── Escenario 5: ruta no está en tránsito ─────────────────────────────────

    @Test
    void lanza_excepcion_cuando_ruta_no_esta_en_transito() {
        Ruta confirmada = Ruta.reconstituir(rutaId, "d3gpz", EstadoRuta.CONFIRMADA, 120.0,
                TipoVehiculo.VAN, vehiculoId, conductorId,
                Instant.now().minusSeconds(7200), Instant.now().plusSeconds(3600),
                null, null, null);
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> useCase.ejecutar(rutaId, false))
                .isInstanceOf(RutaNoEnTransitoException.class);
    }

    // ── Escenario 6: payload RUTA_CERRADA incluye modelo_contrato ────────────

    @Test
    void evento_ruta_cerrada_incluye_modelo_contrato_del_conductor() {
        Ruta ruta = rutaEnTransito();
        Conductor conductor = Conductor.reconstituir(conductorId, "Ana López", "ana@test.com",
                ModeloContrato.POR_PARADA, EstadoConductor.EN_RUTA, vehiculoId);
        Vehiculo vehiculo = vehiculoEnTransito();

        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.of(ruta));
        when(paradaRepository.buscarPorRutaId(rutaId)).thenReturn(List.of(paradaExitosa()));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.of(vehiculo));

        useCase.ejecutar(rutaId, false);

        ArgumentCaptor<RutaCerradaEvent> eventCaptor = ArgumentCaptor.forClass(RutaCerradaEvent.class);
        verify(integracionModulo3).publishRutaCerrada(eventCaptor.capture());
        RutaCerradaEvent event = eventCaptor.getValue();

        assertThat(event.tipoEvento()).isEqualTo("RUTA_CERRADA");
        assertThat(event.tipoCierre()).isEqualTo(TipoCierre.MANUAL.name());
        assertThat(event.conductor().modeloContrato()).isEqualTo(ModeloContrato.POR_PARADA.name());
        assertThat(event.conductor().nombre()).isEqualTo("Ana López");
    }
}
