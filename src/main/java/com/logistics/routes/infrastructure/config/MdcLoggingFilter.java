package com.logistics.routes.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro HTTP que enriquece el MDC (Mapped Diagnostic Context) de SLF4J con claves
 * de trazabilidad por request. Esto permite que cada línea de log incluya automáticamente
 * el contexto de negocio sin que el código de negocio lo tenga que gestionar.
 *
 * <p>Claves que se extraen e inyectan en el MDC:
 * <ul>
 *   <li>{@code request_id}   — UUID único generado por request (correlación entre microservicios)</li>
 *   <li>{@code ruta_id}      — Extraído de la URL si el path contiene /rutas/{id}</li>
 *   <li>{@code conductor_id} — Extraído del header X-Conductor-Id o del path /conductores/{id}</li>
 *   <li>{@code user_email}   — Extraído del Principal autenticado (cuando disponible)</li>
 * </ul>
 *
 * <p>Las claves MDC se limpian en el bloque {@code finally} para evitar contaminación
 * entre requests en entornos con pool de threads (como Tomcat).
 *
 * <p>Para usar en logback.xml o application.yml:
 * <pre>
 *   logging.pattern.console: "%d{HH:mm:ss} [%thread] %-5level [%X{request_id}] [ruta:%X{ruta_id}] [conductor:%X{conductor_id}] %logger{36} - %msg%n"
 * </pre>
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_REQUEST_ID   = "request_id";
    private static final String MDC_RUTA_ID      = "ruta_id";
    private static final String MDC_CONDUCTOR_ID = "conductor_id";
    private static final String MDC_USER         = "user_email";

    // Patrones para extraer IDs de la URL
    private static final Pattern RUTA_ID_PATTERN      = Pattern.compile("/rutas/([0-9a-fA-F\\-]{36})");
    private static final Pattern CONDUCTOR_ID_PATTERN = Pattern.compile("/conductores/([0-9a-fA-F\\-]{36})");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            populateMdc(request);
            filterChain.doFilter(request, response);
        } finally {
            // CRÍTICO: siempre limpiar el MDC para no contaminar el próximo request
            // que reutilice este thread del pool de Tomcat.
            clearMdc();
        }
    }

    private void populateMdc(HttpServletRequest request) {
        // 1. Request ID único — permite correlacionar logs de un mismo request
        String requestId = extractRequestIdHeader(request);
        MDC.put(MDC_REQUEST_ID, requestId);

        // 2. ruta_id — extraído del path URI
        String uri = request.getRequestURI();
        extractUuidFromPath(uri, RUTA_ID_PATTERN)
                .ifPresent(id -> MDC.put(MDC_RUTA_ID, id));

        // 3. conductor_id — del header dedicado (para WebSocket y SQS) o del path URI
        String conductorHeader = request.getHeader("X-Conductor-Id");
        if (conductorHeader != null && !conductorHeader.isBlank()) {
            MDC.put(MDC_CONDUCTOR_ID, conductorHeader);
        } else {
            extractUuidFromPath(uri, CONDUCTOR_ID_PATTERN)
                    .ifPresent(id -> MDC.put(MDC_CONDUCTOR_ID, id));
        }

        // 4. Usuario autenticado — Spring Security lo pone en el request principal
        if (request.getUserPrincipal() != null) {
            MDC.put(MDC_USER, request.getUserPrincipal().getName());
        }
    }

    private void clearMdc() {
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_RUTA_ID);
        MDC.remove(MDC_CONDUCTOR_ID);
        MDC.remove(MDC_USER);
    }

    /**
     * Extrae el X-Request-ID del header si existe; si no, genera uno nuevo.
     * Esto permite que un API Gateway o cliente propague su propio ID de correlación.
     */
    private String extractRequestIdHeader(HttpServletRequest request) {
        String header = request.getHeader("X-Request-Id");
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }

    /**
     * Extrae el primer grupo de captura (UUID) de un patrón regex aplicado al URI.
     */
    private java.util.Optional<String> extractUuidFromPath(String uri, Pattern pattern) {
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return java.util.Optional.of(matcher.group(1));
        }
        return java.util.Optional.empty();
    }
}
