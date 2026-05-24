package com.bitpub.auth.config;

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
 * Configurazione centralizzata OpenAPI 3 per il microservizio auth-service.
 * Espone:
 *   - GET  /v3/api-docs         → spec JSON
 *   - GET  /swagger-ui.html     → Swagger UI
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Auth Service – Local"),
                        new Server().url("http://api-gateway:8080/auth").description("Via API Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, buildJwtSecurityScheme())
                );
    }

    private Info buildInfo() {
        return new Info()
                .title("BitPub — Auth Service API")
                .version("1.0.0")
                .description("""
                        API di autenticazione e autorizzazione della piattaforma **BitPub**.
                        
                        Fornisce endpoint per:
                        - **Registrazione** utente con hash BCrypt
                        - **Login** con emissione JWT firmato HS256
                        
                        Il token JWT restituito deve essere incluso in ogni richiesta ai microservizi protetti
                        nell'header: `Authorization: Bearer <token>`
                        """)
                .contact(new Contact()
                        .name("BitPub Team")
                        .email("dev@bitpub.io")
                        .url("https://github.com/bitpub"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Definisce lo schema di sicurezza JWT Bearer compatibile OpenAPI 3.
     * Viene referenziato da ogni operazione protetta tramite @SecurityRequirement.
     */
    private SecurityScheme buildJwtSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Token JWT ottenuto tramite `POST /api/v1/auth/login`.
                        
                        Formato header: `Authorization: Bearer <token>`
                        
                        Il token contiene: `sub` (username), `role`, `iat`, `exp`.
                        """);
    }
}
