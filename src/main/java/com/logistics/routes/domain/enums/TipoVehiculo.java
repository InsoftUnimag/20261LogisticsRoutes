package com.logistics.routes.domain.enums;

public enum TipoVehiculo {
    MOTO(50.0),
    VAN(300.0),
    NHR(1000.0),
    TURBO(3000.0);

    private final double capacidadKg;

    TipoVehiculo(double capacidadKg) {
        this.capacidadKg = capacidadKg;
    }

    public double capacidadKg() {
        return capacidadKg;
    }

    public TipoVehiculo siguienteTipo() {
        TipoVehiculo[] valores = values();
        int siguiente = ordinal() + 1;
        return siguiente < valores.length ? valores[siguiente] : this;
    }
}
