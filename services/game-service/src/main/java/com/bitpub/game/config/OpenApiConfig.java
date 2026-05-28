package com.bitpub.game.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configurazione centralizzata OpenAPI 3 per il microservizio game-service.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI gameServiceOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Game Service – Local"),
                        new Server().url("http://api-gateway:8080/games").description("Via API Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, buildJwtSecurityScheme())
                );
    }

    private Info buildInfo() {
        return new Info()
                .title("BitPub — Game Service API")
                .version("1.0.0")
                .description("""
                        API di gestione giochi e sessioni della piattaforma **BitPub**.
                        
                        Fornisce endpoint per:
                        - **Listing** dei giochi disponibili nella piattaforma
                        - **Creazione** nuovi giochi (richiede ruolo ADMIN)
                        - Gestione **sessioni di partita** (MatchSession)
                        - Registrazione **dispositivi** (Edge node)
                        
                        Tutti gli endpoint richiedono autenticazione JWT Bearer.
                        """)
                .contact(new Contact()
                        .name("BitPub Team")
                        .email("dev@bitpub.io")
                        .url("https://github.com/bitpub"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme buildJwtSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token JWT ottenuto da `POST /api/v1/auth/login`. Header: `Authorization: Bearer <token>`");
    }
}
