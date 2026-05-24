package com.bitpub.auth.controller;

import com.bitpub.auth.dto.AuthRequest;
import com.bitpub.auth.dto.AuthResponse;
import com.bitpub.auth.dto.RegisterRequest;
import com.bitpub.auth.service.AuthService;
import com.bitpub.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Endpoint di autenticazione e registrazione utenti. Non richiedono JWT."
)
public class AuthController {

    private final AuthService authService;

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/register
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Registra un nuovo utente",
        description = """
            Crea un nuovo account utente nella piattaforma BitPub.
            
            - La password viene hashata con **BCrypt** prima del salvataggio.
            - Il campo `username` deve essere univoco.
            - L'endpoint è pubblico: non richiede JWT.
            - Al termine, restituisce direttamente un JWT valido.
            """,
        security = @SecurityRequirement(name = "") // endpoint pubblico
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Registrazione avvenuta con successo. Restituisce il token JWT.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successo",
                    value = """
                        {
                          "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpb3Jvc3NpIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3MTY1NzYwMDAsImV4cCI6MTcxNjY2MjQwMH0.abc123",
                          "type": "Bearer",
                          "username": "mario_rossi",
                          "role": "USER"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dati di registrazione non validi (campi mancanti o formato errato).",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validazione fallita",
                    value = """
                        {
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Username già in uso",
                          "path": "/api/v1/auth/register",
                          "timestamp": "2024-05-24T21:00:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username già registrato.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Credenziali e dati dell'utente da registrare.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(
                        name = "Esempio registrazione",
                        value = """
                            {
                              "username": "mario_rossi",
                              "password": "Secur3Pass!",
                              "email": "mario@example.com"
                            }
                            """
                    )
                )
            )
            RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Autenticazione utente (login)",
        description = """
            Autentica un utente con username e password.
            
            - Valida le credenziali contro il database utenti.
            - In caso di successo, restituisce un **JWT Bearer token** con scadenza 24h.
            - Il token deve essere usato negli header di tutte le richieste protette.
            - L'endpoint è pubblico: non richiede JWT.
            """,
        security = @SecurityRequirement(name = "") // endpoint pubblico
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login riuscito. Restituisce il token JWT.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Login riuscito",
                    value = """
                        {
                          "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpb3Jvc3NpIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3MTY1NzYwMDAsImV4cCI6MTcxNjY2MjQwMH0.abc123",
                          "type": "Bearer",
                          "username": "mario_rossi",
                          "role": "USER"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corpo della richiesta malformato.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenziali non valide (username o password errati).",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Credenziali errate",
                    value = """
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Username o password non validi",
                          "path": "/api/v1/auth/login",
                          "timestamp": "2024-05-24T21:00:00"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Credenziali dell'utente.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = AuthRequest.class),
                    examples = @ExampleObject(
                        name = "Esempio login",
                        value = """
                            {
                              "username": "mario_rossi",
                              "password": "Secur3Pass!"
                            }
                            """
                    )
                )
            )
            AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
