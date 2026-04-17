package com.logistics.routes.domain.model;

import java.time.Instant;
import java.util.UUID;

public class HistorialAsignacion {

    private final UUID id;
    private final UUID conductorId;
    private final UUID vehiculoId;
    private final Instant fechaInicio;
    private Instant fechaFin;
    private boolean activo;

    private HistorialAsignacion(UUID id, UUID conductorId, UUID vehiculoId,
                                Instant fechaInicio, Instant fechaFin, boolean activo) {
        this.id = id;
        this.conductorId = conductorId;
        this.vehiculoId = vehiculoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
    }

    public static HistorialAsignacion iniciar(UUID conductorId, UUID vehiculoId) {
        return new HistorialAsignacion(UUID.randomUUID(), conductorId, vehiculoId,
                Instant.now(), null, true);
    }

    public void cerrar() {
        this.fechaFin = Instant.now();
        this.activo = false;
    }

    public UUID getId() { return id; }
    public UUID getConductorId() { return conductorId; }
    public UUID getVehiculoId() { return vehiculoId; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public boolean isActivo() { return activo; }
}
