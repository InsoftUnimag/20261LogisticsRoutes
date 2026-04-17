package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.VehiculoEnTransitoException;
import com.logistics.routes.domain.model.Vehiculo;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehiculoTest {

    private ZonaGeografica zonaBogota() {
        return ZonaGeografica.from(4.7110, -74.0721);
    }

    @Test
    void nuevo_vehiculo_tiene_estado_disponible_y_sin_conductor() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford Transit",
                300.0, 10.0, zonaBogota());

        assertEquals(EstadoVehiculo.DISPONIBLE, v.getEstado());
        assertNotNull(v.getId());
        assertNull(v.getConductorId());
    }

    @Test
    void no_permite_capacidad_cero() {
        assertThrows(IllegalArgumentException.class,
                () -> Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                        0.0, 10.0, zonaBogota()));
    }

    @Test
    void no_permite_capacidad_negativa() {
        assertThrows(IllegalArgumentException.class,
                () -> Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                        -5.0, 10.0, zonaBogota()));
    }

    @Test
    void no_permite_volumen_cero_o_negativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                        100.0, 0.0, zonaBogota()));
        assertThrows(IllegalArgumentException.class,
                () -> Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford",
                        100.0, -1.0, zonaBogota()));
    }

    @Test
    void marcarInactivo_falla_si_esta_en_transito() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford Transit",
                300.0, 10.0, zonaBogota());
        v.asignarConductor(UUID.randomUUID());

        assertEquals(EstadoVehiculo.EN_TRANSITO, v.getEstado());
        assertThrows(VehiculoEnTransitoException.class, v::marcarInactivo);
    }

    @Test
    void marcarInactivo_exitoso_si_esta_disponible() {
        Vehiculo v = Vehiculo.nuevo("ABC123", TipoVehiculo.VAN, "Ford Transit",
                300.0, 10.0, zonaBogota());

        assertDoesNotThrow(v::marcarInactivo);
        assertEquals(EstadoVehiculo.INACTIVO, v.getEstado());
    }

    @Test
    void asignarConductor_cambia_estado_a_en_transito() {
        Vehiculo v = Vehiculo.nuevo("XYZ456", TipoVehiculo.NHR, "Chevrolet NHR",
                1000.0, 15.0, zonaBogota());
        UUID conductorId = UUID.randomUUID();

        v.asignarConductor(conductorId);

        assertEquals(EstadoVehiculo.EN_TRANSITO, v.getEstado());
        assertEquals(conductorId, v.getConductorId());
    }

    @Test
    void desvincularConductor_restaura_estado_disponible() {
        Vehiculo v = Vehiculo.nuevo("XYZ456", TipoVehiculo.NHR, "Chevrolet NHR",
                1000.0, 15.0, zonaBogota());
        v.asignarConductor(UUID.randomUUID());

        v.desvincularConductor();

        assertEquals(EstadoVehiculo.DISPONIBLE, v.getEstado());
        assertNull(v.getConductorId());
    }
}
