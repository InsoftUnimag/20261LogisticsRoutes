package com.logistics.routes.infrastructure.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.logistics.routes.domain.exception.ConductorNoAsignadoARutaException;
import com.logistics.routes.domain.exception.ConductorNoDisponibleException;
import com.logistics.routes.domain.exception.ConductorNoEncontradoException;
import com.logistics.routes.domain.exception.ConductorYaAsignadoException;
import com.logistics.routes.domain.exception.DominioException;
import com.logistics.routes.domain.exception.EmailDuplicadoException;
import com.logistics.routes.domain.exception.FechaLimiteVencidaException;
import com.logistics.routes.domain.exception.ParadaNoEncontradaException;
import com.logistics.routes.domain.exception.PlacaDuplicadaException;
import com.logistics.routes.domain.exception.RutaEstadoInvalidoException;
import com.logistics.routes.domain.exception.RutaNoEncontradaException;
import com.logistics.routes.domain.exception.RutaNoEnTransitoException;
import com.logistics.routes.domain.exception.VehiculoEnTransitoException;
import com.logistics.routes.domain.exception.VehiculoNoDisponibleException;
import com.logistics.routes.domain.exception.VehiculoNoEncontradoException;
import com.logistics.routes.infrastructure.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejador global de excepciones de la API.
 *
 * <p>Responsabilidades en este Sprint 0:
 * <ul>
 *   <li>Validaciones de Bean Validation (@Valid) → 422 Unprocessable Entity</li>
 *   <li>Body JSON malformado o enum inválido → 400 Bad Request</li>
 *   <li>Acceso denegado por Security → 403 Forbidden</li>
 *   <li>Cualquier RuntimeException no mapeada → 500 Internal Server Error</li>
 * </ul>
 *
 * <p>A medida que los Use Cases del Sprint 1 se integren, se agregarán aquí
 * los @ExceptionHandler específicos de dominio (PlacaDuplicadaException → 409, etc.).
 * NO agregar handlers de dominio en esta feature — orden de ejecución del equipo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Excepciones de dominio ────────────────────────────────────────────────

    @ExceptionHandler(PlacaDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handlePlacaDuplicada(PlacaDuplicadaException ex) {
        return ErrorResponse.of("PLACA_DUPLICADA", ex.getMessage());
    }

    @ExceptionHandler(VehiculoEnTransitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleVehiculoEnTransito(VehiculoEnTransitoException ex) {
        return ErrorResponse.of("VEHICULO_EN_TRANSITO", ex.getMessage());
    }

    @ExceptionHandler(VehiculoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleVehiculoNoEncontrado(VehiculoNoEncontradoException ex) {
        return ErrorResponse.of("VEHICULO_NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(VehiculoNoDisponibleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleVehiculoNoDisponible(VehiculoNoDisponibleException ex) {
        return ErrorResponse.of("VEHICULO_NO_DISPONIBLE", ex.getMessage());
    }

    @ExceptionHandler(ConductorNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleConductorNoEncontrado(ConductorNoEncontradoException ex) {
        return ErrorResponse.of("CONDUCTOR_NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(ConductorYaAsignadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConductorYaAsignado(ConductorYaAsignadoException ex) {
        return ErrorResponse.of("CONDUCTOR_YA_ASIGNADO", ex.getMessage());
    }

    @ExceptionHandler(ConductorNoDisponibleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConductorNoDisponible(ConductorNoDisponibleException ex) {
        return ErrorResponse.of("CONDUCTOR_NO_DISPONIBLE", ex.getMessage());
    }

    @ExceptionHandler(ConductorNoAsignadoARutaException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleConductorNoAsignadoARuta(ConductorNoAsignadoARutaException ex) {
        return ErrorResponse.of("CONDUCTOR_NO_ASIGNADO_A_RUTA", ex.getMessage());
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailDuplicado(EmailDuplicadoException ex) {
        return ErrorResponse.of("EMAIL_DUPLICADO", ex.getMessage());
    }

    @ExceptionHandler(RutaNoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRutaNoEncontrada(RutaNoEncontradaException ex) {
        return ErrorResponse.of("RUTA_NO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(ParadaNoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleParadaNoEncontrada(ParadaNoEncontradaException ex) {
        return ErrorResponse.of("PARADA_NO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(FechaLimiteVencidaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleFechaLimiteVencida(FechaLimiteVencidaException ex) {
        return ErrorResponse.of("FECHA_LIMITE_VENCIDA", ex.getMessage());
    }

    @ExceptionHandler(RutaEstadoInvalidoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleRutaEstadoInvalido(RutaEstadoInvalidoException ex) {
        return ErrorResponse.of("RUTA_ESTADO_INVALIDO", ex.getMessage());
    }

    @ExceptionHandler(RutaNoEnTransitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleRutaNoEnTransito(RutaNoEnTransitoException ex) {
        return ErrorResponse.of("RUTA_NO_EN_TRANSITO", ex.getMessage());
    }

    /** Fallback para cualquier excepción de dominio no mapeada explícitamente. */
    @ExceptionHandler(DominioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDominioException(DominioException ex) {
        log.warn("Excepción de dominio no mapeada: {}", ex.getMessage());
        return ErrorResponse.of("DOMINIO_ERROR", ex.getMessage());
    }

    // ── Validaciones y errores HTTP ───────────────────────────────────────────

    /**
     * Maneja fallos de validación de @Valid/@Validated en los request bodies.
     * Retorna 422 con la lista detallada de campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        log.warn("Validación fallida: {} errores de campo", detalles.size());

        return ErrorResponse.of(
                "VALIDATION_ERROR",
                "El cuerpo de la solicitud contiene campos inválidos",
                detalles
        );
    }

    /**
     * Maneja path variables o query params cuyo valor no puede convertirse
     * al tipo esperado (ej: UUID mal formado, enum con valor inválido).
     * Retorna 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "desconocido";
        String detalle = "Valor inválido '%s' para el parámetro '%s'. Se esperaba tipo %s."
                .formatted(ex.getValue(), ex.getName(), requiredType);
        log.warn("Type mismatch en parámetro: {}", detalle);
        return ErrorResponse.of("INVALID_PARAMETER", detalle);
    }

    /**
     * Maneja bodies JSON malformados (JSON roto, tipos incompatibles, enums inválidos).
     * Retorna 400 Bad Request con un mensaje descriptivo.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        String detalle = "Cuerpo de la solicitud ilegible o mal formado";

        if (ex.getCause() instanceof InvalidFormatException ife) {
            detalle = "Valor inválido '%s' para el campo '%s'"
                    .formatted(ife.getValue(), ife.getPath().isEmpty()
                            ? "desconocido"
                            : ife.getPath().getLast().getFieldName());
        }

        log.warn("Request no legible: {}", ex.getMessage());

        return ErrorResponse.of("BAD_REQUEST", detalle);
    }

    /**
     * Maneja AccessDeniedException lanzada por Spring Security cuando el usuario
     * no tiene el rol requerido para acceder al endpoint.
     * Retorna 403 Forbidden.
     *
     * <p>NOTA: Spring Security maneja su propio 401 (no autenticado) a nivel de filtro,
     * por lo que aquí solo llegamos cuando el usuario SÍ está autenticado pero NO autorizado.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ErrorResponse.of("ACCESS_DENIED", "No tienes permisos para realizar esta operación");
    }

    /**
     * Maneja excepciones de autenticación (como BadCredentialsException) que ocurren
     * explícitamente en los endpoints públicos, como el /login.
     * Retorna 401 Unauthorized.
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Error de autenticación: {}", ex.getMessage());
        return ErrorResponse.of("UNAUTHORIZED", "Credenciales inválidas");
    }

    /**
     * Fallback genérico para cualquier RuntimeException no mapeada explícitamente.
     * Retorna 500 Internal Server Error y loguea el stack trace completo para análisis.
     *
     * <p>En producción estos errores deben ser corregidos — NO exponer detalles de la excepción
     * al cliente por razones de seguridad.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        log.error("Error inesperado no manejado: {}", ex.getMessage(), ex);
        return ErrorResponse.of(
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Contacta al equipo de soporte."
        );
    }
}
