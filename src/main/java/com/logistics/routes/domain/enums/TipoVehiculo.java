package com.logistics.routes.domain.enums;

import java.util.Optional;

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

    public Optional<TipoVehiculo> siguienteTipo() {
        TipoVehiculo[] valores = values();
        int siguiente = ordinal() + 1;
        return siguiente < valores.length
                ? Optional.of(valores[siguiente])
                : Optional.empty();
    }

    public boolean porcentajeExcede(double pesoActual, double umbralPorcentaje) {
        return pesoActual > capacidadKg * (umbralPorcentaje / 100.0);
    }
}
