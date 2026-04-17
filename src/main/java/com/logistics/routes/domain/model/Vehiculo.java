package com.logistics.routes.domain.model;

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
    private ZonaGeografica zonaOperacion;
    private EstadoVehiculo estado;
    private UUID conductorId;

    private Vehiculo(UUID id, String placa, TipoVehiculo tipo, String modelo,
                     double capacidadPesoKg, double volumenMaximoM3,
                     ZonaGeografica zonaOperacion, EstadoVehiculo estado,
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
                capacidadPesoKg, volumenMaximoM3, zonaOperacion,
                EstadoVehiculo.DISPONIBLE, null);
    }

    public void actualizar(String modelo, double capacidadPesoKg, double volumenMaximoM3,
                           ZonaGeografica zonaOperacion) {
        this.modelo = modelo;
        this.capacidadPesoKg = capacidadPesoKg;
        this.volumenMaximoM3 = volumenMaximoM3;
        this.zonaOperacion = zonaOperacion;
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

    public UUID getId() { return id; }
    public String getPlaca() { return placa; }
    public TipoVehiculo getTipo() { return tipo; }
    public String getModelo() { return modelo; }
    public double getCapacidadPesoKg() { return capacidadPesoKg; }
    public double getVolumenMaximoM3() { return volumenMaximoM3; }
    public ZonaGeografica getZonaOperacion() { return zonaOperacion; }
    public EstadoVehiculo getEstado() { return estado; }
    public UUID getConductorId() { return conductorId; }
}
