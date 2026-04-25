package com.logistics.routes.application.port.out;

import com.logistics.routes.application.usecase.RutaCerradaEvent;

/**
 * Puerto de salida hacia Módulo 3 (Liquidación).
 * Publica el evento RUTA_CERRADA con el payload completo de SPEC-08 sección 4.
 * La implementación real se provee por SQS bajo el perfil "aws" (F22);
 * en dev/test se usa un stub InMemory que loguea el evento.
 */
public interface IntegracionModulo3Port {

    void publishRutaCerrada(RutaCerradaEvent event);
}
