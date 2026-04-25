package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta del endpoint {@code GET /api/conductor/ruta-activa}.
 * Incluye la información que el conductor necesita para operar en campo:
 * estado de la ruta, fecha de inicio de tránsito y paradas con todos los
 * datos del paquete (mercancía, método de pago, fecha límite).
 */
public record RutaActivaResponse(
        UUID id,
        String zona,
        EstadoRuta estado,
        TipoVehiculo tipoVehiculoRequerido,
        UUID conductorId,
        UUID vehiculoId,
        Instant fechaLimiteDespacho,
        Instant fechaHoraInicio,
        List<ParadaConductorItem> paradas
) {

    public record ParadaConductorItem(
            UUID id,
            UUID paqueteId,
            int orden,
            String direccion,
            double latitud,
            double longitud,
            String tipoMercancia,
            String metodoPago,
            Instant fechaLimiteEntrega,
            EstadoParada estado,
            MotivoNovedad motivoNovedad,
            Instant fechaHoraGestion,
            String fotoEvidenciaUrl,
            String firmaReceptorUrl,
            String nombreReceptor
    ) {
        public static ParadaConductorItem from(Parada p) {
            return new ParadaConductorItem(
                    p.getId(),
                    p.getPaqueteId(),
                    p.getOrden(),
                    p.getDireccion(),
                    p.getLatitud(),
                    p.getLongitud(),
                    p.getTipoMercancia(),
                    p.getMetodoPago(),
                    p.getFechaLimiteEntrega(),
                    p.getEstado(),
                    p.getMotivoNovedad(),
                    p.getFechaHoraGestion(),
                    p.getFotoEvidenciaUrl(),
                    p.getFirmaReceptorUrl(),
                    p.getNombreReceptor()
            );
        }
    }

    public static RutaActivaResponse from(Ruta ruta, List<Parada> paradas) {
        return new RutaActivaResponse(
                ruta.getId(),
                ruta.getZona(),
                ruta.getEstado(),
                ruta.getTipoVehiculoRequerido(),
                ruta.getConductorId(),
                ruta.getVehiculoId(),
                ruta.getFechaLimiteDespacho(),
                ruta.getFechaHoraInicio(),
                paradas.stream()
                        .map(ParadaConductorItem::from)
                        .toList()
        );
    }
}
