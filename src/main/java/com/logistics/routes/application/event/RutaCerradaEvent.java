package com.logistics.routes.application.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.logistics.routes.domain.model.Conductor;
import com.logistics.routes.domain.model.Parada;
import com.logistics.routes.domain.model.Ruta;
import com.logistics.routes.domain.model.Vehiculo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload del evento RUTA_CERRADA enviado a Módulo 3 (SPEC-08 sección 4).
 * El JSON resultante usa snake_case según el contrato de integración.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RutaCerradaEvent(
        String tipoEvento,
        UUID rutaId,
        String tipoCierre,
        Instant fechaHoraInicioTransito,
        Instant fechaHoraCierre,
        ConductorInfo conductor,
        VehiculoInfo vehiculo,
        List<ParadaInfo> paradas
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConductorInfo(UUID conductorId, String nombre, String modeloContrato) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record VehiculoInfo(UUID vehiculoId, String placa, String tipo) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ParadaInfo(UUID paqueteId, String estado, String motivoNoEntrega,
                             Instant fechaHoraGestion) {}

    public static RutaCerradaEvent from(Ruta ruta, Conductor conductor,
                                        Vehiculo vehiculo, List<Parada> paradas) {
        return new RutaCerradaEvent(
                "RUTA_CERRADA",
                ruta.getId(),
                ruta.getTipoCierre().name(),
                ruta.getFechaHoraInicio(),
                ruta.getFechaHoraCierre(),
                new ConductorInfo(
                        conductor.getId(),
                        conductor.getNombre(),
                        conductor.getModeloContrato().name()),
                new VehiculoInfo(
                        vehiculo.getId(),
                        vehiculo.getPlaca(),
                        vehiculo.getTipo().name()),
                paradas.stream()
                        .map(p -> new ParadaInfo(
                                p.getPaqueteId(),
                                p.getEstado().name(),
                                p.getMotivoNovedad() != null ? p.getMotivoNovedad().name() : null,
                                p.getFechaHoraGestion()))
                        .toList()
        );
    }
}
