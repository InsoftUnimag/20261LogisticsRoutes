# =============================================================================
# Multi-stage build — logistics-routes
# Stage 1: compila el JAR con Gradle
# Stage 2: imagen final liviana solo con JRE
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar solo los archivos de dependencias primero (mejor cache de capas)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

# Descargar dependencias (esta capa se cachea si build.gradle no cambia)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# código fuente y compilar
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]