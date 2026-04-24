package com.logistics.routes.application.usecase;

import com.logistics.routes.application.command.ConfirmarDespachoCommand;
import com.logistics.routes.application.port.out.ConductorRepositoryPort;
import com.logistics.routes.application.port.out.ParadaRepositoryPort;
import com.logistics.routes.application.port.out.RutaRepositoryPort;
import com.logistics.routes.application.port.out.VehiculoRepositoryPort;
import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.VehiculoNoDisponibleException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmarDespachoUseCaseTest {

    @Mock RutaRepositoryPort rutaRepository;
    @Mock VehiculoRepositoryPort vehiculoRepository;
    @Mock ConductorRepositoryPort conductorRepository;
    @Mock ParadaRepositoryPort paradaRepository;

    ConfirmarDespachoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmarDespachoUseCase(
                rutaRepository, vehiculoRepository, conductorRepository, paradaRepository);
    }

    @Test
    void confirma_despacho_actualiza_estados_y_optimiza_paradas() {
        // Given: ruta LISTA_PARA_DESPACHO + conductor ACTIVO + vehículo DISPONIBLE MOTO
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        Vehiculo vehiculo = vehiculoDisponibleMoto("ABC123");
        List<Parada> paradas = paradasDesordenadas(ruta.getId());

        mockFlujoFeliz(ruta, conductor, vehiculo, paradas);

        // When
        Ruta resultado = useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), vehiculo.getId()));

        // Then: estados transicionaron correctamente
        assertThat(resultado.getEstado()).isEqualTo(EstadoRuta.CONFIRMADA);
        assertThat(resultado.getConductorId()).isEqualTo(conductor.getId());
        assertThat(resultado.getVehiculoId()).isEqualTo(vehiculo.getId());
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.EN_TRANSITO);
        assertThat(conductor.getEstado()).isEqualTo(EstadoConductor.EN_RUTA);

        verify(rutaRepository).guardar(ruta);
        verify(vehiculoRepository).guardar(vehiculo);
        verify(conductorRepository).guardar(conductor);
        verify(paradaRepository).guardarTodas(any());
    }

    @Test
    void nearest_neighbor_reordena_paradas_desde_la_primera() {
        // Given: tres paradas donde el orden óptimo difiere del orden de entrada
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        Vehiculo vehiculo = vehiculoDisponibleMoto("ABC123");

        Parada a = Parada.nueva(ruta.getId(), UUID.randomUUID(), "A", 11.240, -74.200, null, null, null);
        Parada b = Parada.nueva(ruta.getId(), UUID.randomUUID(), "B", 11.241, -74.200, null, null, null);
        Parada c = Parada.nueva(ruta.getId(), UUID.randomUUID(), "C", 11.340, -74.200, null, null, null);

        // Entrada: A (ancla) → C → B. Nearest-neighbor debería resultar en A → B → C.
        mockFlujoFeliz(ruta, conductor, vehiculo, List.of(a, c, b));

        // When
        useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), vehiculo.getId()));

        // Then: orden 1=A, 2=B (más cercana a A), 3=C
        assertThat(a.getOrden()).isEqualTo(1);
        assertThat(b.getOrden()).isEqualTo(2);
        assertThat(c.getOrden()).isEqualTo(3);
    }

    @Test
    void lanza_excepcion_cuando_la_ruta_no_existe() {
        UUID rutaId = UUID.randomUUID();
        when(rutaRepository.buscarPorId(rutaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(rutaId,
                new ConfirmarDespachoCommand(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(RutaNoEncontradaException.class);

        verify(rutaRepository, never()).guardar(any());
    }

    @Test
    void lanza_excepcion_cuando_el_conductor_no_existe() {
        Ruta ruta = rutaListaParaDespacho();
        UUID conductorId = UUID.randomUUID();
        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductorId, UUID.randomUUID())))
                .isInstanceOf(ConductorNoEncontradoException.class);
    }

    @Test
    void lanza_excepcion_cuando_el_conductor_no_esta_activo() {
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        conductor.marcarInactivo();

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductor.getId())).thenReturn(Optional.of(conductor));

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), UUID.randomUUID())))
                .isInstanceOf(ConductorNoDisponibleException.class);
    }

    @Test
    void lanza_excepcion_cuando_el_vehiculo_no_existe() {
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        UUID vehiculoId = UUID.randomUUID();

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductor.getId())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), vehiculoId)))
                .isInstanceOf(VehiculoNoEncontradoException.class);
    }

    @Test
    void lanza_excepcion_cuando_el_vehiculo_no_esta_disponible() {
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        Vehiculo vehiculo = vehiculoDisponibleMoto("ABC123");
        vehiculo.marcarEnTransito();

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductor.getId())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculo.getId())).thenReturn(Optional.of(vehiculo));

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), vehiculo.getId())))
                .isInstanceOf(VehiculoNoDisponibleException.class);
    }

    @Test
    void lanza_excepcion_cuando_el_tipo_de_vehiculo_no_coincide_con_la_ruta() {
        // La ruta requiere MOTO (default), pero el vehículo es VAN
        Ruta ruta = rutaListaParaDespacho();
        Conductor conductor = Conductor.nuevo("Juan Pérez", "juan@logistica.com", ModeloContrato.POR_PARADA);
        Vehiculo vehiculo = Vehiculo.nuevo("VAN001", TipoVehiculo.VAN, "Renault Kangoo",
                500.0, 4.5, ZonaGeografica.from(11.240, -74.199));

        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductor.getId())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculo.getId())).thenReturn(Optional.of(vehiculo));

        assertThatThrownBy(() -> useCase.ejecutar(ruta.getId(),
                new ConfirmarDespachoCommand(conductor.getId(), vehiculo.getId())))
                .isInstanceOf(VehiculoNoDisponibleException.class)
                .hasMessageContaining("tipo");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Ruta rutaListaParaDespacho() {
        Ruta ruta = Ruta.nueva("d29ej", Instant.now().plus(5, ChronoUnit.DAYS));
        ruta.transicionarAListaParaDespacho();
        return ruta;
    }

    private Vehiculo vehiculoDisponibleMoto(String placa) {
        return Vehiculo.nuevo(placa, TipoVehiculo.MOTO, "Yamaha NMX 155",
                20.0, 0.45, ZonaGeografica.from(11.240, -74.199));
    }

    private List<Parada> paradasDesordenadas(UUID rutaId) {
        return List.of(
                Parada.nueva(rutaId, UUID.randomUUID(), "Dirección 1",
                        11.240, -74.200, null, null, null),
                Parada.nueva(rutaId, UUID.randomUUID(), "Dirección 2",
                        11.250, -74.210, null, null, null)
        );
    }

    private void mockFlujoFeliz(Ruta ruta, Conductor conductor, Vehiculo vehiculo, List<Parada> paradas) {
        when(rutaRepository.buscarPorId(ruta.getId())).thenReturn(Optional.of(ruta));
        when(conductorRepository.buscarPorId(conductor.getId())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.buscarPorId(vehiculo.getId())).thenReturn(Optional.of(vehiculo));
        when(paradaRepository.buscarPorRutaId(ruta.getId())).thenReturn(paradas);
        when(paradaRepository.guardarTodas(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehiculoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conductorRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}
