package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.TipoVehiculo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TipoVehiculoTest {

    // ── Capacidades ──────────────────────────────────────────────────────

    @Test
    void capacidades_correctas() {
        assertEquals(50.0, TipoVehiculo.MOTO.capacidadKg());
        assertEquals(300.0, TipoVehiculo.VAN.capacidadKg());
        assertEquals(1000.0, TipoVehiculo.NHR.capacidadKg());
        assertEquals(3000.0, TipoVehiculo.TURBO.capacidadKg());
    }

    // ── siguienteTipo ────────────────────────────────────────────────────

    @Test
    void siguienteTipo_sigue_cadena_correcta() {
        assertEquals(Optional.of(TipoVehiculo.VAN), TipoVehiculo.MOTO.siguienteTipo());
        assertEquals(Optional.of(TipoVehiculo.NHR), TipoVehiculo.VAN.siguienteTipo());
        assertEquals(Optional.of(TipoVehiculo.TURBO), TipoVehiculo.NHR.siguienteTipo());
    }

    @Test
    void siguienteTipo_turbo_retorna_empty() {
        assertEquals(Optional.empty(), TipoVehiculo.TURBO.siguienteTipo());
    }

    // ── porcentajeExcede ─────────────────────────────────────────────────

    @Test
    void porcentajeExcede_true_cuando_supera_umbral() {
        // MOTO cap=50, 90% de 50 = 45. Peso 46 excede el 90%.
        assertTrue(TipoVehiculo.MOTO.porcentajeExcede(46.0, 90.0));
    }

    @Test
    void porcentajeExcede_false_cuando_no_supera_umbral() {
        // MOTO cap=50, 90% de 50 = 45. Peso 44 no excede.
        assertFalse(TipoVehiculo.MOTO.porcentajeExcede(44.0, 90.0));
    }

    @Test
    void porcentajeExcede_false_en_el_limite_exacto() {
        // MOTO cap=50, 90% de 50 = 45. Peso exacto 45 no excede (no es >).
        assertFalse(TipoVehiculo.MOTO.porcentajeExcede(45.0, 90.0));
    }

    @Test
    void porcentajeExcede_con_van_al_100() {
        // VAN cap=300, 100% = 300. Peso 301 excede.
        assertTrue(TipoVehiculo.VAN.porcentajeExcede(301.0, 100.0));
        assertFalse(TipoVehiculo.VAN.porcentajeExcede(300.0, 100.0));
    }
}
