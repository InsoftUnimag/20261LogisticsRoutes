package com.logistics.routes.domain.enums;

public enum ModeloContrato {
    RECORRIDO_COMPLETO("Recorrido completo"),
    POR_PARADA("Por Parada Realizada");

    private final String descripcion;

    ModeloContrato(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Texto descriptivo legible para integraciones externas (ej: Módulo 3).
     * No mezclar con {@link #name()} — los nombres del enum se usan en la
     * persistencia, la API REST y las colas con M1.
     */
    public String descripcion() {
        return descripcion;
    }
}
