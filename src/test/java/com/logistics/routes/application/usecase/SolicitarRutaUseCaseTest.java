package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.SolicitarRutaCommand;
import com.logistics.routes.application.port.out.NotificacionDespachadorPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.FechaLimiteVencidaException;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import com.logistics.routes.infrastructure.helper.RutaCreadorTransaccional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitarRutaUseCaseTest {

    // Coordenadas en Bogotá — zona "d29ej" (geohash precisión 5)
    private static final double LAT = 4.7109;
    private static final double LON = -74.0721;

    @Mock RutaRepositoryPort rutaRepository;
    @Mock ParadaRepositoryPort paradaRepository;
    @Mock NotificacionDespachadorPort notificacion;
    @Mock RutaCreadorTransaccional rutaCreador;

    SolicitarRutaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SolicitarRutaUseCase(rutaRepository, paradaRepository, notificacion, rutaCreador);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SolicitarRutaCommand commandValido(double pesoKg) {
        return new SolicitarRutaCommand(
                UUID.randomUUID(), pesoKg, LAT, LON,
                "Calle 72 #10-25, Bogotá", "GENERAL", "EFECTIVO",
                Instant.now().plus(3, ChronoUnit.DAYS)
        );
    }

    private Ruta rutaCreadaConPeso(double pesoActual, TipoVehiculo tipo) {
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);
        Ruta ruta = Ruta.nueva(zona.hash(), Instant.now().plus(5, ChronoUnit.DAYS));
        if (pesoActual > 0) {
            ruta.agregarPeso(pesoActual);
        }
        if (tipo != TipoVehiculo.MOTO) {
            ruta.setTipoVehiculoRequerido(tipo);
        }
        return ruta;
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    void paquete_se_agrega_a_ruta_creada_existente_en_la_zona() {
        // Given: existe ruta CREADA con 5 kg en la misma zona (MOTO, cap 50 kg)
        Ruta existente = rutaCreadaConPeso(5.0, TipoVehiculo.MOTO);
        UUID rutaId = existente.getId();
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);

        when(rutaRepository.buscarRutaActivaPorZona(zona)).thenReturn(Optional.of(existente));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: llega paquete de 5 kg
        UUID resultado = useCase.ejecutar(commandValido(5.0));

        // Then: misma ruta, peso acumulado = 10 kg
        assertThat(resultado).isEqualTo(rutaId);
        assertThat(existente.getPesoAcumuladoKg()).isEqualTo(10.0);
        assertThat(existente.getEstado()).isEqualTo(EstadoRuta.CREADA);
        verify(rutaCreador, never()).guardarNueva(any());
        verify(paradaRepository, times(1)).guardar(any(Parada.class));
    }

    @Test
    void crea_nueva_ruta_cuando_no_existe_ninguna_en_la_zona() {
        // Given: ninguna ruta CREADA en la zona
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);
        Ruta nueva = Ruta.nueva(zona.hash(), Instant.now().plus(5, ChronoUnit.DAYS));

        when(rutaRepository.buscarRutaActivaPorZona(zona)).thenReturn(Optional.empty());
        when(rutaCreador.guardarNueva(any())).thenReturn(nueva);
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: llega paquete de 3 kg
        UUID resultado = useCase.ejecutar(commandValido(3.0));

        // Then: ruta nueva en CREADA con MOTO y peso = 3 kg
        assertThat(resultado).isEqualTo(nueva.getId());
        assertThat(nueva.getEstado()).isEqualTo(EstadoRuta.CREADA);
        assertThat(nueva.getTipoVehiculoRequerido()).isEqualTo(TipoVehiculo.MOTO);
        assertThat(nueva.getPesoAcumuladoKg()).isEqualTo(3.0);
        verify(rutaCreador, times(1)).guardarNueva(any());

        // Y la fecha límite es ~5 días a partir de ahora
        ArgumentCaptor<Ruta> captor = ArgumentCaptor.forClass(Ruta.class);
        verify(rutaCreador).guardarNueva(captor.capture());
        Instant limite = captor.getValue().getFechaLimiteDespacho();
        assertThat(limite).isAfter(Instant.now().plus(4, ChronoUnit.DAYS));
    }

    @Test
    void escala_tipo_vehiculo_a_van_al_superar_90_porciento_de_moto() {
        // Given: ruta con 44 kg en MOTO (capacidad 50 kg → 90% = 45 kg)
        Ruta existente = rutaCreadaConPeso(44.0, TipoVehiculo.MOTO);
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);

        when(rutaRepository.buscarRutaActivaPorZona(zona)).thenReturn(Optional.of(existente));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: llega paquete de 2 kg → total 46 kg → supera 90% de 50 kg
        useCase.ejecutar(commandValido(2.0));

        // Then: tipo escala a VAN, ruta sigue en CREADA
        assertThat(existente.getTipoVehiculoRequerido()).isEqualTo(TipoVehiculo.VAN);
        assertThat(existente.getEstado()).isEqualTo(EstadoRuta.CREADA);
        verify(notificacion, never()).notificarRutaListaParaDespacho(any(), any(), anyDouble(), any(), any());
    }

    @Test
    void transiciona_a_lista_para_despacho_al_superar_90_porciento_de_turbo() {
        // Given: ruta con 2700 kg en TURBO (capacidad 3000 kg → 90% = 2700 kg)
        Ruta existente = rutaCreadaConPeso(2700.0, TipoVehiculo.TURBO);
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);

        when(rutaRepository.buscarRutaActivaPorZona(zona)).thenReturn(Optional.of(existente));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: llega paquete de 1 kg → total 2701 kg → supera 90% de 3000 kg
        useCase.ejecutar(commandValido(1.0));

        // Then: ruta transiciona a LISTA_PARA_DESPACHO y notifica
        assertThat(existente.getEstado()).isEqualTo(EstadoRuta.LISTA_PARA_DESPACHO);
        verify(notificacion, times(1)).notificarRutaListaParaDespacho(
                eq(existente.getId()), eq(existente.getZona()),
                eq(2701.0), eq(TipoVehiculo.TURBO), eq("capacidad_maxima_alcanzada")
        );
    }

    @Test
    void rechaza_paquete_con_fecha_limite_vencida_y_lanza_excepcion() {
        // Given: paquete con fechaLimiteEntrega en el pasado
        SolicitarRutaCommand command = new SolicitarRutaCommand(
                UUID.randomUUID(), 5.0, LAT, LON,
                "Calle 72 #10-25, Bogotá", "GENERAL", "EFECTIVO",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        // When / Then: lanza FechaLimiteVencidaException sin crear ni modificar rutas
        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(FechaLimiteVencidaException.class);

        verify(rutaRepository, never()).buscarRutaActivaPorZona(any());
        verify(rutaRepository, never()).guardar(any());
        verify(paradaRepository, never()).guardar(any());
        verify(notificacion, times(1)).notificarAlertaPrioritaria(any());
    }

    @Test
    void paquete_sin_fecha_limite_entrega_se_acepta_sin_restriccion() {
        // Given: comando sin fechaLimiteEntrega (null)
        SolicitarRutaCommand command = new SolicitarRutaCommand(
                UUID.randomUUID(), 5.0, LAT, LON,
                "Calle 72 #10-25, Bogotá", null, null, null
        );
        ZonaGeografica zona = ZonaGeografica.from(LAT, LON);
        Ruta nueva = Ruta.nueva(zona.hash(), Instant.now().plus(5, ChronoUnit.DAYS));

        when(rutaRepository.buscarRutaActivaPorZona(zona)).thenReturn(Optional.empty());
        when(rutaCreador.guardarNueva(any())).thenReturn(nueva);
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When / Then: no lanza excepción
        UUID resultado = useCase.ejecutar(command);
        assertThat(resultado).isEqualTo(nueva.getId());
    }
}
