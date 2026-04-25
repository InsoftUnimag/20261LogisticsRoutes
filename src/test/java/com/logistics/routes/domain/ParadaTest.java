package com.logistics.routes.domain;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.OrigenParada;
import com.logistics.routes.domain.exception.ParadaSinPODException;
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

    // ── Helpers ──────────────────────────────────────────────────────────

    private Parada paradaPendiente() {
        return Parada.nueva(rutaId(), paqueteId(), "Calle 45 #12-30",
                4.7110, -74.0721, "ESTANDAR", "PREPAGO",
                Instant.now().plus(2, ChronoUnit.DAYS));
    }

    // ── marcarExitosa ───────────────────────────────────────────────────

    @Test
    void marcarExitosa_con_foto_actualiza_estado_y_campos_pod() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();
        Instant ahora = Instant.now();

        // When: se marca exitosa con foto, firma y receptor
        p.marcarExitosa("http://foto.jpg", "http://firma.png", "Juan Pérez", ahora);

        // Then: estado EXITOSA, campos POD asignados, origen CONDUCTOR
        assertEquals(EstadoParada.EXITOSA, p.getEstado());
        assertEquals("http://foto.jpg", p.getFotoEvidenciaUrl());
        assertEquals("http://firma.png", p.getFirmaReceptorUrl());
        assertEquals("Juan Pérez", p.getNombreReceptor());
        assertEquals(ahora, p.getFechaHoraGestion());
        assertEquals(OrigenParada.CONDUCTOR, p.getOrigen());
    }

    @Test
    void marcarExitosa_sin_foto_lanza_ParadaSinPODException() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();

        // When/Then: marcar exitosa sin foto lanza excepción
        assertThrows(ParadaSinPODException.class,
                () -> p.marcarExitosa(null, "http://firma.png", "Juan", Instant.now()));
    }

    @Test
    void marcarExitosa_con_foto_vacia_lanza_ParadaSinPODException() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();

        // When/Then: marcar exitosa con foto vacía lanza excepción
        assertThrows(ParadaSinPODException.class,
                () -> p.marcarExitosa("  ", "http://firma.png", "Juan", Instant.now()));
    }

    // ── marcarFallida ───────────────────────────────────────────────────

    @Test
    void marcarFallida_actualiza_estado_motivo_y_origen() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();
        Instant ahora = Instant.now();

        // When: se marca fallida con motivo
        p.marcarFallida(MotivoNovedad.CLIENTE_AUSENTE, ahora);

        // Then: estado FALLIDA, motivo asignado, origen CONDUCTOR
        assertEquals(EstadoParada.FALLIDA, p.getEstado());
        assertEquals(MotivoNovedad.CLIENTE_AUSENTE, p.getMotivoNovedad());
        assertEquals(ahora, p.getFechaHoraGestion());
        assertEquals(OrigenParada.CONDUCTOR, p.getOrigen());
    }

    // ── marcarNovedad ───────────────────────────────────────────────────

    @Test
    void marcarNovedad_actualiza_estado_motivo_y_origen() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();
        Instant ahora = Instant.now();

        // When: se marca como novedad
        p.marcarNovedad(MotivoNovedad.ZONA_DIFICIL_ACCESO, ahora);

        // Then: estado NOVEDAD, motivo asignado, origen CONDUCTOR
        assertEquals(EstadoParada.NOVEDAD, p.getEstado());
        assertEquals(MotivoNovedad.ZONA_DIFICIL_ACCESO, p.getMotivoNovedad());
        assertEquals(ahora, p.getFechaHoraGestion());
        assertEquals(OrigenParada.CONDUCTOR, p.getOrigen());
    }

    // ── marcarSinGestion ────────────────────────────────────────────────

    @Test
    void marcarSinGestion_actualiza_estado_y_origen_sistema() {
        // Given: una parada pendiente
        Parada p = paradaPendiente();
        Instant ahora = Instant.now();

        // When: el sistema marca sin gestión
        p.marcarSinGestion(ahora);

        // Then: estado SIN_GESTION_CONDUCTOR, origen SISTEMA
        assertEquals(EstadoParada.SIN_GESTION_CONDUCTOR, p.getEstado());
        assertEquals(ahora, p.getFechaHoraGestion());
        assertEquals(OrigenParada.SISTEMA, p.getOrigen());
    }

    // ── Validación de estado PENDIENTE ──────────────────────────────────

    @Test
    void marcarExitosa_falla_si_parada_no_esta_pendiente() {
        Parada p = paradaPendiente();
        p.marcarFallida(MotivoNovedad.CLIENTE_AUSENTE, Instant.now());

        assertThrows(IllegalStateException.class,
                () -> p.marcarExitosa("http://foto.jpg", null, "Juan", Instant.now()));
    }

    @Test
    void marcarFallida_falla_si_parada_no_esta_pendiente() {
        Parada p = paradaPendiente();
        p.marcarExitosa("http://foto.jpg", null, "Juan", Instant.now());

        assertThrows(IllegalStateException.class,
                () -> p.marcarFallida(MotivoNovedad.CLIENTE_AUSENTE, Instant.now()));
    }

    @Test
    void marcarNovedad_falla_si_parada_no_esta_pendiente() {
        Parada p = paradaPendiente();
        p.marcarNovedad(MotivoNovedad.EXTRAVIADO, Instant.now());

        assertThrows(IllegalStateException.class,
                () -> p.marcarNovedad(MotivoNovedad.EXTRAVIADO, Instant.now()));
    }

    // ── esPODValido ─────────────────────────────────────────────────────

    @Test
    void esPODValido_retorna_true_cuando_tiene_foto() {
        // Given: una parada marcada exitosa con foto
        Parada p = paradaPendiente();
        p.marcarExitosa("http://foto.jpg", null, null, Instant.now());

        // When/Then: POD es válido
        assertTrue(p.esPODValido());
    }

    @Test
    void esPODValido_retorna_false_cuando_no_tiene_foto() {
        // Given: una parada pendiente sin foto
        Parada p = paradaPendiente();

        // When/Then: POD no es válido
        assertFalse(p.esPODValido());
    }
}
