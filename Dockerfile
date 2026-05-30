# =============================================================================
# Dockerfile — logistics-routes (Spring Boot 3.4.1 / Java 21)
#
# Build multi-etapa:
#   1. builder  → compila el proyecto con Gradle y genera el fat-JAR
#   2. runtime  → imagen mínima JRE que solo corre el JAR
#
# Las credenciales AWS las provee el IAM Role del EC2 (no se hardcodean aquí).
# =============================================================================

# ── Etapa 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiamos wrapper primero → capa cacheada si build.gradle no cambia
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# gradlew viene de Windows con CRLF → sed elimina el \r para que Linux lo ejecute
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Descargamos dependencias en capa separada (se cachea entre builds)
RUN ./gradlew dependencies --no-daemon -q

# Copiamos código fuente y compilamos (sin tests — ya corren en CI/local)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Etapa 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl → necesario para el healthcheck de docker compose
RUN apk add --no-cache curl

# Usuario sin privilegios de root (buena práctica de seguridad)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Solo copiamos el JAR desde el builder
COPY --from=builder /app/build/libs/logistics-routes-*.jar app.jar

EXPOSE 8080

# -XX:MaxRAMPercentage=75.0  → JVM respeta el límite de memoria del contenedor
# -Djava.security.egd        → aceleración de arranque en Linux
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
