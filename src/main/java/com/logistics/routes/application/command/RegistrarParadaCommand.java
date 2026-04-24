package com.logistics.routes.application.command;

import com.logistics.routes.domain.enums.MotivoNovedad;

import java.time.Instant;
import java.util.UUID;

public sealed interface RegistrarParadaCommand permits
        RegistrarParadaCommand.Exitosa,
        RegistrarParadaCommand.Fallida,
        RegistrarParadaCommand.Novedad {

    UUID paradaId();

    record Exitosa(
            UUID paradaId,
            String fotoUrl,
            String firmaUrl,
            String nombreReceptor,
            Instant fechaAccion
    ) implements RegistrarParadaCommand {}

    record Fallida(
            UUID paradaId,
            MotivoNovedad motivo,
            Instant fechaAccion
    ) implements RegistrarParadaCommand {}

    record Novedad(
            UUID paradaId,
            MotivoNovedad tipoNovedad,
            Instant fechaAccion
    ) implements RegistrarParadaCommand {}
}
