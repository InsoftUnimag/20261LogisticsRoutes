package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;
import com.logistics.routes.domain.model.Conductor;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConductorTest {

    @Test
    void nuevo_conductor_tiene_estado_activo_sin_vehiculo() {
        Conductor c = Conductor.nuevo("Juan Pérez", "juan@mail.com", ModeloContrato.POR_PARADA);

        assertEquals(EstadoConductor.ACTIVO, c.getEstado());
        assertNull(c.getVehiculoAsignadoId());
        assertNotNull(c.getId());
    }

    @Test
    void asignarVehiculo_exitoso_cuando_conductor_activo_y_sin_vehiculo() {
        Conductor c = Conductor.nuevo("Ana López", "ana@mail.com", ModeloContrato.RECORRIDO_COMPLETO);
        UUID vehiculoId = UUID.randomUUID();

        assertDoesNotThrow(() -> c.asignarVehiculo(vehiculoId));
        assertEquals(vehiculoId, c.getVehiculoAsignadoId());
    }

    @Test
    void asignarVehiculo_falla_si_ya_tiene_vehiculo_asignado() {
        Conductor c = Conductor.nuevo("Ana López", "ana@mail.com", ModeloContrato.RECORRIDO_COMPLETO);
        c.asignarVehiculo(UUID.randomUUID());

        assertThrows(ConductorYaAsignadoException.class,
                () -> c.asignarVehiculo(UUID.randomUUID()));
    }

    @Test
    void asignarVehiculo_falla_si_conductor_inactivo() {
        Conductor c = Conductor.nuevo("Pedro García", "pedro@mail.com", ModeloContrato.POR_PARADA);
        c.marcarInactivo();

        assertThrows(ConductorNoDisponibleException.class,
                () -> c.asignarVehiculo(UUID.randomUUID()));
    }

    @Test
    void asignarVehiculo_falla_si_conductor_en_ruta() {
        Conductor c = Conductor.nuevo("Pedro García", "pedro@mail.com", ModeloContrato.POR_PARADA);
        c.marcarEnRuta();

        assertThrows(ConductorNoDisponibleException.class,
                () -> c.asignarVehiculo(UUID.randomUUID()));
    }

    @Test
    void desvincularVehiculo_retorna_uuid_previo() {
        Conductor c = Conductor.nuevo("María Torres", "maria@mail.com", ModeloContrato.RECORRIDO_COMPLETO);
        UUID vehiculoId = UUID.randomUUID();
        c.asignarVehiculo(vehiculoId);

        Optional<UUID> resultado = c.desvincularVehiculo();

        assertTrue(resultado.isPresent());
        assertEquals(vehiculoId, resultado.get());
        assertNull(c.getVehiculoAsignadoId());
    }

    @Test
    void desvincularVehiculo_retorna_optional_vacio_cuando_no_hay_vehiculo() {
        Conductor c = Conductor.nuevo("María Torres", "maria@mail.com", ModeloContrato.RECORRIDO_COMPLETO);

        Optional<UUID> resultado = c.desvincularVehiculo();

        assertFalse(resultado.isPresent());
    }

    @Test
    void cambios_de_estado_funcionan_correctamente() {
        Conductor c = Conductor.nuevo("Luis Gómez", "luis@mail.com", ModeloContrato.POR_PARADA);

        c.marcarEnRuta();
        assertEquals(EstadoConductor.EN_RUTA, c.getEstado());

        c.marcarActivo();
        assertEquals(EstadoConductor.ACTIVO, c.getEstado());

        c.marcarInactivo();
        assertEquals(EstadoConductor.INACTIVO, c.getEstado());
    }
}
