package com.logistics.routes.infrastructure.adapter.out.integration;

import com.logistics.routes.application.port.out.IntegracionModulo1Port;
import com.logistics.routes.domain.enums.TipoCierre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stub en memoria de la integración con Módulo 1 (SGP).
 * Loguea cada evento con el payload plano para trazabilidad.
 * Reemplazar por implementación SQS real bajo el perfil "aws" en F22.
 */
@Component
public class InMemoryIntegracionModulo1Adapter implements IntegracionModulo1Port {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIntegracionModulo1Adapter.class);

    @Override
    public void publishPaqueteExcluidoDespacho(UUID paqueteId, UUID rutaId, String motivo, Instant fechaHora) {
        log.info("[M1-EVENT] PAQUETE_EXCLUIDO_DESPACHO paquete_id={} ruta_id={} motivo='{}' timestamp={}",
                paqueteId, rutaId, motivo, fechaHora);
    }

    @Override
    public void publishPaqueteEnTransito(UUID paqueteId, UUID rutaId, Instant fechaHora) {
        log.info("[M1-EVENT] PAQUETE_EN_TRANSITO paquete_id={} ruta_id={} timestamp={}",
                paqueteId, rutaId, fechaHora);
    }

    @Override
    public void publishPaqueteEntregado(UUID paqueteId, UUID rutaId, Instant fechaEntrega,
                                        String urlFoto, String urlFirma) {
        log.info("[M1-EVENT] PAQUETE_ENTREGADO paquete_id={} ruta_id={} timestamp={} url_foto='{}' url_firma='{}'",
                paqueteId, rutaId, fechaEntrega, urlFoto, urlFirma);
    }

    @Override
    public void publishParadaFallida(UUID paqueteId, UUID rutaId, String motivo, Instant fechaAccion) {
        log.info("[M1-EVENT] PARADA_FALLIDA paquete_id={} ruta_id={} motivo='{}' timestamp={}",
                paqueteId, rutaId, motivo, fechaAccion);
    }

    @Override
    public void publishNovedadGrave(UUID paqueteId, UUID rutaId, String tipoNovedad, Instant fechaAccion) {
        log.info("[M1-EVENT] NOVEDAD_GRAVE paquete_id={} ruta_id={} tipo='{}' timestamp={}",
                paqueteId, rutaId, tipoNovedad, fechaAccion);
    }

    @Override
    public void publishParadasSinGestionar(UUID rutaId, TipoCierre tipoCierre, List<UUID> paqueteIds) {
        log.info("[M1-EVENT] PARADAS_SIN_GESTIONAR ruta_id={} tipo_cierre={} paquetes={}",
                rutaId, tipoCierre, paqueteIds);
    }
}
