package com.logistics.routes.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto de salida hacia Módulo 1 (Sistema de Gestión de Paquetes).
 * Publica eventos del ciclo de vida de paquetes según SPEC-08.
 * La implementación real se provee por SQS bajo el perfil "aws" (F22);
 * en dev/test se usa un stub InMemory que loguea los eventos.
 */
public interface IntegracionModulo1Port {

    /**
     * Evento #9 SPEC-08: el Despachador excluyó el paquete de una ruta en
     * LISTA_PARA_DESPACHO antes de confirmar el despacho.
     */
    void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora);

    /** Evento SPEC-08: paquete en tránsito al iniciar la ruta. */
    void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHora);

    /** Evento SPEC-08: paquete entregado exitosamente con evidencia POD (foto + firma). */
    void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fechaEntrega,
                                 String urlFoto, String urlFirma);

    /** Evento SPEC-08: parada fallida con motivo. */
    void publishParadaFallida(UUID paqueteId, UUID rutaId, String motivo, Instant fechaAccion);

    /** Evento SPEC-08: novedad grave registrada por el conductor. */
    void publishNovedadGrave(UUID paqueteId, UUID rutaId, String tipoNovedad, Instant fechaAccion);
}
