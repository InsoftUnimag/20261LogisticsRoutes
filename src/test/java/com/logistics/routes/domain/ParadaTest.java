package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.model.Parada;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParadaTest {

    private UUID rutaId() { return UUID.randomUUID(); }
    private UUID paqueteId() { return UUID.randomUUID(); }

    // ── Factory nueva ────────────────────────────────────────────────────

    @Test
    void nueva_parada_tiene_estado_pendiente_y_origen_sistema() {
        Parada p = Parada.nueva(
                rutaId(), paqueteId(), "Calle 45 #12-30, Bogotá",
                4.7110, -74.0721, "ESTANDAR", "PREPAGO",
                Instant.now().plus(2, ChronoUnit.DAYS));

        assertEquals(EstadoParada.PENDIENTE, p.getEstado());
        assertEquals(OrigenParada.SISTEMA, p.getOrigen());
        assertEquals(0, p.getOrden());
        assertNotNull(p.getId());
    }

    @Test
    void nueva_genera_ids_unicos() {
        UUID ruta = rutaId();
        UUID paquete1 = paqueteId();
        UUID paquete2 = paqueteId();

        Parada p1 = Parada.nueva(ruta, paquete1, "Dir 1", 4.71, -74.07,
                "ESTANDAR", "PREPAGO", null);
        Parada p2 = Parada.nueva(ruta, paquete2, "Dir 2", 4.72, -74.08,
                "FRAGIL", "CONTRA_ENTREGA", null);

        assertNotEquals(p1.getId(), p2.getId());
    }

    @Test
    void nueva_falla_con_rutaId_nulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Parada.nueva(null, paqueteId(), "Dir", 4.71, -74.07,
                        "ESTANDAR", "PREPAGO", null));
    }

    @Test
    void nueva_falla_con_paqueteId_nulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Parada.nueva(rutaId(), null, "Dir", 4.71, -74.07,
                        "ESTANDAR", "PREPAGO", null));
    }

    @Test
    void nueva_falla_con_direccion_vacia() {
        assertThrows(IllegalArgumentException.class,
                () -> Parada.nueva(rutaId(), paqueteId(), "  ", 4.71, -74.07,
                        "ESTANDAR", "PREPAGO", null));
    }

    @Test
    void nueva_acepta_fecha_limite_nula() {
        Parada p = Parada.nueva(rutaId(), paqueteId(), "Calle 10",
                4.71, -74.07, "ESTANDAR", "PREPAGO", null);

        assertNull(p.getFechaLimiteEntrega());
    }
}
