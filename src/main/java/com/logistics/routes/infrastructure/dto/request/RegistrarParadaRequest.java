package com.logistics.routes.infrastructure.dto.request;

import com.logistics.routes.application.command.RegistrarParadaCommand;
import com.logistics.routes.domain.enums.MotivoNovedad;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request para registrar el resultado de una parada.
 * <p>
 * El campo {@code tipo} discrimina la variante del comando:
 * <ul>
 *   <li>EXITOSA → requiere {@code fotoUrl} (POD obligatorio)</li>
 *   <li>FALLIDA / NOVEDAD → requieren {@code motivo}</li>
 * </ul>
 * {@code fechaAccion} es el timestamp del conductor (soporte offline) — el backend
 * lo respeta y NO usa {@code Instant.now()}.
 */
public record RegistrarParadaRequest(
        @NotNull TipoResultado tipo,
        String fotoUrl,
        String firmaUrl,
        String nombreReceptor,
        MotivoNovedad motivo,
        @NotNull Instant fechaAccion
) {

    public enum TipoResultado { EXITOSA, FALLIDA, NOVEDAD }

    public RegistrarParadaCommand toCommand(UUID paradaId) {
        return switch (tipo) {
            case EXITOSA -> new RegistrarParadaCommand.Exitosa(
                    paradaId, fotoUrl, firmaUrl, nombreReceptor, fechaAccion);
            case FALLIDA -> new RegistrarParadaCommand.Fallida(
                    paradaId, motivo, fechaAccion);
            case NOVEDAD -> new RegistrarParadaCommand.Novedad(
                    paradaId, motivo, fechaAccion);
        };
    }
}
