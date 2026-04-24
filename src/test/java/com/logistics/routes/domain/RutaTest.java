package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Ruta;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RutaTest {

    private Instant maniana() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    // ── Factory nueva ────────────────────────────────────────────────────

    @Test
    void nueva_ruta_tiene_estado_creada_peso_cero_tipo_moto() {
        // Given / When
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        // Then
        assertNotNull(ruta.getId());
        assertEquals("d2g7x", ruta.getZona());
        assertEquals(EstadoRuta.CREADA, ruta.getEstado());
        assertEquals(0.0, ruta.getPesoAcumuladoKg());
        assertEquals(TipoVehiculo.MOTO, ruta.getTipoVehiculoRequerido());
        assertNull(ruta.getVehiculoId());
        assertNull(ruta.getConductorId());
        assertNotNull(ruta.getFechaCreacionRuta());
        assertNotNull(ruta.getFechaLimiteDespacho());
        assertNull(ruta.getFechaHoraInicio());
        assertNull(ruta.getFechaHoraCierre());
        assertNull(ruta.getTipoCierre());
    }

    @Test
    void nueva_falla_con_zona_nula() {
        assertThrows(IllegalArgumentException.class,
                () -> Ruta.nueva(null, maniana()));
    }

    @Test
    void nueva_falla_con_zona_vacia() {
        assertThrows(IllegalArgumentException.class,
                () -> Ruta.nueva("  ", maniana()));
    }

    @Test
    void nueva_falla_con_fecha_limite_nula() {
        assertThrows(IllegalArgumentException.class,
                () -> Ruta.nueva("d2g7x", null));
    }

    // ── agregarPeso ──────────────────────────────────────────────────────

    @Test
    void agregarPeso_exitoso_en_estado_creada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        ruta.agregarPeso(10.5);

        assertEquals(10.5, ruta.getPesoAcumuladoKg());
    }

    @Test
    void agregarPeso_acumula_multiples_llamadas() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        ruta.agregarPeso(10.0);
        ruta.agregarPeso(5.5);

        assertEquals(15.5, ruta.getPesoAcumuladoKg());
    }

    @Test
    void agregarPeso_falla_con_peso_cero() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        assertThrows(IllegalArgumentException.class,
                () -> ruta.agregarPeso(0));
    }

    @Test
    void agregarPeso_falla_con_peso_negativo() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        assertThrows(IllegalArgumentException.class,
                () -> ruta.agregarPeso(-3.0));
    }

    @Test
    void agregarPeso_falla_si_no_esta_creada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();

        assertThrows(IllegalStateException.class,
                () -> ruta.agregarPeso(5.0));
    }

    // ── setTipoVehiculoRequerido ─────────────────────────────────────────

    @Test
    void setTipo_upgrade_de_moto_a_van() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        ruta.setTipoVehiculoRequerido(TipoVehiculo.VAN);

        assertEquals(TipoVehiculo.VAN, ruta.getTipoVehiculoRequerido());
    }

    @Test
    void setTipo_upgrade_multiples_niveles() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        ruta.setTipoVehiculoRequerido(TipoVehiculo.TURBO);

        assertEquals(TipoVehiculo.TURBO, ruta.getTipoVehiculoRequerido());
    }

    @Test
    void setTipo_mismo_tipo_permitido() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        assertDoesNotThrow(() -> ruta.setTipoVehiculoRequerido(TipoVehiculo.MOTO));
    }

    @Test
    void setTipo_downgrade_lanza_excepcion() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.setTipoVehiculoRequerido(TipoVehiculo.NHR);

        assertThrows(IllegalArgumentException.class,
                () -> ruta.setTipoVehiculoRequerido(TipoVehiculo.VAN));
    }

    @Test
    void setTipo_falla_si_no_esta_creada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();

        assertThrows(IllegalStateException.class,
                () -> ruta.setTipoVehiculoRequerido(TipoVehiculo.VAN));
    }

    @Test
    void setTipo_nulo_lanza_excepcion() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        assertThrows(IllegalArgumentException.class,
                () -> ruta.setTipoVehiculoRequerido(null));
    }

    // ── transicionarAListaParaDespacho ────────────────────────────────────

    @Test
    void transicionar_a_lista_desde_creada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        ruta.transicionarAListaParaDespacho();

        assertEquals(EstadoRuta.LISTA_PARA_DESPACHO, ruta.getEstado());
    }

    @Test
    void transicionar_a_lista_falla_desde_lista() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();

        assertThrows(IllegalStateException.class,
                ruta::transicionarAListaParaDespacho);
    }

    @Test
    void transicionar_a_lista_falla_desde_confirmada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();
        ruta.confirmar(UUID.randomUUID(), UUID.randomUUID());

        assertThrows(IllegalStateException.class,
                ruta::transicionarAListaParaDespacho);
    }

    // ── confirmar ────────────────────────────────────────────────────────

    @Test
    void confirmar_exitosa_desde_lista_para_despacho() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();
        UUID conductor = UUID.randomUUID();
        UUID vehiculo = UUID.randomUUID();

        ruta.confirmar(conductor, vehiculo);

        assertEquals(EstadoRuta.CONFIRMADA, ruta.getEstado());
        assertEquals(conductor, ruta.getConductorId());
        assertEquals(vehiculo, ruta.getVehiculoId());
    }

    @Test
    void confirmar_falla_desde_creada() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());

        assertThrows(IllegalStateException.class,
                () -> ruta.confirmar(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void confirmar_falla_con_conductor_nulo() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();

        assertThrows(IllegalArgumentException.class,
                () -> ruta.confirmar(null, UUID.randomUUID()));
    }

    @Test
    void confirmar_falla_con_vehiculo_nulo() {
        Ruta ruta = Ruta.nueva("d2g7x", maniana());
        ruta.transicionarAListaParaDespacho();

        assertThrows(IllegalArgumentException.class,
                () -> ruta.confirmar(UUID.randomUUID(), null));
    }
}
