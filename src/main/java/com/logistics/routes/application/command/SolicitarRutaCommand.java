package com.logistics.routes.application.command;

import java.time.Instant;
import java.util.UUID;

public record SolicitarRutaCommand(
        UUID paqueteId,
        double pesoKg,
        double latitud,
        double longitud,
        String direccion,
        String tipoMercancia,
        String metodoPago,
        Instant fechaLimiteEntrega
) {}
