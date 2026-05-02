package com.logistics.routes.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3 / Swagger UI.
 *
 * <p>
 * Acceso local: http://localhost:8080/swagger-ui.html
 * <p>
 * JSON spec: http://localhost:8080/api-docs
 *
 * <p>
 * Incluye esquema de autenticación Bearer (JWT) para que los endpoints
 * protegidos puedan ser probados directamente desde la UI de Swagger usando
 * el botón "Authorize".
 */
@Configuration
public class SwaggerConfig {

        private static final String BEARER_SCHEME_NAME = "BearerAuth";

        @Bean
        public OpenAPI logisticsOpenAPI() {
                return new OpenAPI()
                                .info(buildApiInfo())
                                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                                .components(new Components()
                                                .addSecuritySchemes(BEARER_SCHEME_NAME, buildBearerSecurityScheme()));
        }

        private Info buildApiInfo() {
                return new Info()
                                .title("Módulo 2 — Planificación y Gestión de Rutas y Flota")
                                .version("0.0.1-SNAPSHOT")
                                .description("""
                                                API REST del módulo de logística de rutas.

                                                Cubre la planificación automática de rutas, gestión de flota (vehículos
                                                y conductores), despacho de rutas y operación en campo por parte del conductor.

                                                **Roles disponibles:**
                                                - `ROLE_FLEET_ADMIN` — Gestión de vehículos y conductores
                                                - `ROLE_DISPATCHER`  — Despacho y supervisión de rutas
                                                - `ROLE_DRIVER`      — Operación en campo (conductor)
                                                - `ROLE_SYSTEM`      — Integración entre módulos (SQS)
                                                """)
                                .contact(new Contact()
                                                .name("Equipo Insoft - UniMag")
                                                .email("dev@insoft.unimagdalena.edu.co"));
        }

        /**
         * Define el esquema de seguridad Bearer JWT para que Swagger UI muestre
         * el botón "Authorize" y permita ingresar el token.
         */
        private SecurityScheme buildBearerSecurityScheme() {
                return new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa el token JWT obtenido de POST /api/auth/login. Formato: Bearer {token}");
        }
}
