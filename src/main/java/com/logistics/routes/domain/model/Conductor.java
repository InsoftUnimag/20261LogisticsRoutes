package com.logistics.routes.domain.model;

import com.logistics.routes.domain.enums.EstadoConductor;
import com.logistics.routes.domain.enums.ModeloContrato;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;

import java.util.Optional;
import java.util.UUID;

public class Conductor {

    private final UUID id;
    private String nombre;
    private String email;
    private ModeloContrato modeloContrato;
    private EstadoConductor estado;
    private UUID vehiculoAsignadoId;

    private Conductor(UUID id, String nombre, String email, ModeloContrato modeloContrato,
                      EstadoConductor estado, UUID vehiculoAsignadoId) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.modeloContrato = modeloContrato;
        this.estado = estado;
        this.vehiculoAsignadoId = vehiculoAsignadoId;
    }

    public static Conductor nuevo(String nombre, String email, ModeloContrato modeloContrato) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del conductor es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email del conductor es obligatorio");
        }
        if (modeloContrato == null) {
            throw new IllegalArgumentException("El modelo de contrato es obligatorio");
        }
        return new Conductor(UUID.randomUUID(), nombre, email, modeloContrato,
                EstadoConductor.ACTIVO, null);
    }

    public static Conductor reconstituir(UUID id, String nombre, String email,
                                         ModeloContrato modeloContrato, EstadoConductor estado,
                                         UUID vehiculoAsignadoId) {
        return new Conductor(id, nombre, email, modeloContrato, estado, vehiculoAsignadoId);
    }

    public void asignarVehiculo(UUID vehiculoId) {
        if (vehiculoAsignadoId != null) {
            throw new ConductorYaAsignadoException(id.toString());
        }
        if (estado != EstadoConductor.ACTIVO) {
            throw new ConductorNoDisponibleException(id.toString());
        }
        this.vehiculoAsignadoId = vehiculoId;
    }

    public Optional<UUID> desvincularVehiculo() {
        UUID anterior = vehiculoAsignadoId;
        this.vehiculoAsignadoId = null;
        return Optional.ofNullable(anterior);
    }

    public void marcarInactivo() {
        this.estado = EstadoConductor.INACTIVO;
    }

    public void marcarEnRuta() {
        this.estado = EstadoConductor.EN_RUTA;
    }

    public void marcarActivo() {
        this.estado = EstadoConductor.ACTIVO;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public ModeloContrato getModeloContrato() { return modeloContrato; }
    public EstadoConductor getEstado() { return estado; }
    public UUID getVehiculoAsignadoId() { return vehiculoAsignadoId; }
}
