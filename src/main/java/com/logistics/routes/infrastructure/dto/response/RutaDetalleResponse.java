package com.logistics.routes.infrastructure.dto.response;

import com.logistics.routes.domain.enums.EstadoRuta;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RutaDetalleResponse(
        UUID id,
        String zona,
        EstadoRuta estado,
        TipoVehiculo tipoVehiculoRequerido,
        double pesoAcumuladoKg,
        UUID conductorId,
        UUID vehiculoId,
        Instant fechaCreacionRuta,
        Instant fechaLimiteDespacho,
        int cantidadParadas,
        List<ParadaResponse> paradas
) {
    public static RutaDetalleResponse from(Ruta ruta, List<Parada> paradas) {
        List<ParadaResponse> items = paradas.stream()
                .map(ParadaResponse::from)
                .toList();
        return new RutaDetalleResponse(
                ruta.getId(),
                ruta.getZona(),
                ruta.getEstado(),
                ruta.getTipoVehiculoRequerido(),
                ruta.getPesoAcumuladoKg(),
                ruta.getConductorId(),
                ruta.getVehiculoId(),
                ruta.getFechaCreacionRuta(),
                ruta.getFechaLimiteDespacho(),
                items.size(),
                items
        );
    }
}
