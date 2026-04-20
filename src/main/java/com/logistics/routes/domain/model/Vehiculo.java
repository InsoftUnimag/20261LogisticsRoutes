package com.logistics.routes.domain.model;

import com.logistics.routes.application.command.ActualizarVehiculoCommand;
import com.logistics.routes.application.command.RegistrarVehiculoCommand;
import com.logistics.routes.domain.enums.EstadoVehiculo;
import com.logistics.routes.domain.enums.TipoVehiculo;
import com.logistics.routes.domain.exception.VehiculoEnTransitoException;
import com.logistics.routes.domain.valueobject.ZonaGeografica;

import java.util.UUID;

public class Vehiculo {

    private final UUID id;
    private String placa;
    private TipoVehiculo tipo;
    private String modelo;
    private double capacidadPesoKg;
    private double volumenMaximoM3;
    private String zonaOperacion;   // geohash raw — distintas precisiones según contexto
    private EstadoVehiculo estado;
    private UUID conductorId;

    private Vehiculo(UUID id, String placa, TipoVehiculo tipo, String modelo,
                     double capacidadPesoKg, double volumenMaximoM3,
                     String zonaOperacion, EstadoVehiculo estado,
                     UUID conductorId) {
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.modelo = modelo;
        this.capacidadPesoKg = capacidadPesoKg;
        this.volumenMaximoM3 = volumenMaximoM3;
        this.zonaOperacion = zonaOperacion;
        this.estado = estado;
        this.conductorId = conductorId;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    /** Factory original — compatible con los tests de dominio existentes. */
    public static Vehiculo nuevo(String placa, TipoVehiculo tipo, String modelo,
                                 double capacidadPesoKg, double volumenMaximoM3,
                                 ZonaGeografica zonaOperacion) {
        if (capacidadPesoKg <= 0) {
            throw new IllegalArgumentException("La capacidad de peso debe ser mayor a 0");
        }
        if (volumenMaximoM3 <= 0) {
            throw new IllegalArgumentException("El volumen máximo debe ser mayor a 0");
        }
        return new Vehiculo(UUID.randomUUID(), placa, tipo, modelo,
                capacidadPesoKg, volumenMaximoM3, zonaOperacion.hash(),
                EstadoVehiculo.DISPONIBLE, null);
    }

    /** Factory para la capa de aplicación — construye desde el command. */
    public static Vehiculo nuevo(RegistrarVehiculoCommand command) {
        if (command.capacidadPesoKg().doubleValue() <= 0) {
            throw new IllegalArgumentException("La capacidad de peso debe ser mayor a 0");
        }
        if (command.volumenMaximoM3().doubleValue() <= 0) {
            throw new IllegalArgumentException("El volumen máximo debe ser mayor a 0");
        }
        return new Vehiculo(UUID.randomUUID(), command.placa(), command.tipo(), command.modelo(),
                command.capacidadPesoKg().doubleValue(), command.volumenMaximoM3().doubleValue(),
                command.zonaOperacion(), EstadoVehiculo.DISPONIBLE, null);
    }

    /**
     * Factory para la capa de persistencia — reconstruye el agregado desde BD.
     * No aplica invariantes de creación (el dato ya fue validado al crearse).
     */
    public static Vehiculo reconstituir(UUID id, String placa, TipoVehiculo tipo, String modelo,
                                        double capacidadPesoKg, double volumenMaximoM3,
                                        String zonaOperacion, EstadoVehiculo estado,
                                        UUID conductorId) {
        return new Vehiculo(id, placa, tipo, modelo, capacidadPesoKg, volumenMaximoM3,
                zonaOperacion, estado, conductorId);
    }

    // ── Comportamiento de dominio ─────────────────────────────────────────────

    /** Actualización desde command — incluye tipo. */
    public void actualizar(ActualizarVehiculoCommand command) {
        if (this.estado == EstadoVehiculo.EN_TRANSITO) {
            throw new VehiculoEnTransitoException(id.toString());
        }
        this.tipo = command.tipo();
        this.modelo = command.modelo();
        this.capacidadPesoKg = command.capacidadPesoKg().doubleValue();
        this.volumenMaximoM3 = command.volumenMaximoM3().doubleValue();
        this.zonaOperacion = command.zonaOperacion();
    }

    /** Actualización legacy — mantiene compatibilidad con tests de dominio. */
    public void actualizar(String modelo, double capacidadPesoKg, double volumenMaximoM3,
                           ZonaGeografica zonaOperacion) {
        this.modelo = modelo;
        this.capacidadPesoKg = capacidadPesoKg;
        this.volumenMaximoM3 = volumenMaximoM3;
        this.zonaOperacion = zonaOperacion.hash();
    }

    public void marcarInactivo() {
        if (estado == EstadoVehiculo.EN_TRANSITO) {
            throw new VehiculoEnTransitoException(id.toString());
        }
        this.estado = EstadoVehiculo.INACTIVO;
    }

    public void asignarConductor(UUID conductorId) {
        this.conductorId = conductorId;
        this.estado = EstadoVehiculo.EN_TRANSITO;
    }

    public void desvincularConductor() {
        this.conductorId = null;
        this.estado = EstadoVehiculo.DISPONIBLE;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public String getPlaca()             { return placa; }
    public TipoVehiculo getTipo()        { return tipo; }
    public String getModelo()            { return modelo; }
    public double getCapacidadPesoKg()   { return capacidadPesoKg; }
    public double getVolumenMaximoM3()   { return volumenMaximoM3; }
    public String getZonaOperacion()     { return zonaOperacion; }
    public EstadoVehiculo getEstado()    { return estado; }
    public UUID getConductorId()         { return conductorId; }
}
