package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoParada;
import com.logistics.routes.domain.enums.MotivoNovedad;
import com.logistics.routes.domain.model.Parada;

import java.time.Instant;
import java.util.UUID;

public record ParadaResponse(
        UUID id,
        UUID paqueteId,
        int orden,
        String direccion,
        double latitud,
        double longitud,
        EstadoParada estado,
        MotivoNovedad motivoNovedad,
        Instant fechaHoraGestion,
        String fotoEvidenciaUrl,
        String firmaReceptorUrl,
        String nombreReceptor
) {
    public static ParadaResponse from(Parada p) {
        return new ParadaResponse(
                p.getId(),
                p.getPaqueteId(),
                p.getOrden(),
                p.getDireccion(),
                p.getLatitud(),
                p.getLongitud(),
                p.getEstado(),
                p.getMotivoNovedad(),
                p.getFechaHoraGestion(),
                p.getFotoEvidenciaUrl(),
                p.getFirmaReceptorUrl(),
                p.getNombreReceptor()
        );
    }
}
