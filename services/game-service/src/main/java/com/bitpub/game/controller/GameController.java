package com.bitpub.game.controller;

import com.bitpub.common.dto.ErrorResponse;
import com.bitpub.game.model.Game;
import com.bitpub.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bitpub.common.security.enums.Role;
import com.bitpub.common.security.annotations.RequireRole;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(
    name = "Games",
    description = "Gestione del catalogo giochi disponibili nella piattaforma BitPub."
)
@SecurityRequirement(name = "bearerAuth")
public class GameController {

    private final GameService gameService;

    // -------------------------------------------------------------------------
    // GET /api/v1/games
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Lista tutti i giochi",
        description = """
            Restituisce l'elenco completo dei giochi disponibili nella piattaforma.
            
            Ogni gioco include: ID, nome, descrizione, genere, immagine e stato attivo.
            Non richiede privilegi particolari, solo autenticazione JWT.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista giochi restituita con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = Game.class)),
                examples = @ExampleObject(
                    name = "Lista giochi",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "name": "Chess Master",
                            "description": "Scacchi online multiplayer",
                            "genre": "STRATEGY",
                            "active": true
                          },
                          {
                            "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                            "name": "Ping Pong Arena",
                            "description": "Ping pong arcade 2D",
                            "genre": "SPORT",
                            "active": true
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT mancante o non valido.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Errore interno del server.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Crea un nuovo gioco",
        description = """
            Aggiunge un nuovo gioco al catalogo della piattaforma.
            
            **Richiede ruolo ADMIN.**
            
            Il campo `id` viene generato automaticamente dal server (UUID v4) e non deve essere
            incluso nel corpo della richiesta.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Gioco creato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Game.class),
                examples = @ExampleObject(
                    name = "Gioco creato",
                    value = """
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440099",
                          "name": "Battle Royale",
                          "description": "Modalità battle royale 100 giocatori",
                          "genre": "ACTION",
                          "active": true
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corpo della richiesta non valido.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT mancante o non valido.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accesso negato: ruolo ADMIN richiesto.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @RequireRole(Role.PLATFORM_ADMIN)
    @PostMapping
    public ResponseEntity<Game> createGame(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dati del gioco da creare. Il campo `id` verrà ignorato se presente.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = Game.class),
                    examples = @ExampleObject(
                        name = "Nuovo gioco",
                        value = """
                            {
                              "name": "Battle Royale",
                              "description": "Modalità battle royale 100 giocatori",
                              "genre": "ACTION",
                              "active": true
                            }
                            """
                    )
                )
            )
            @RequestBody Game game) {
        return ResponseEntity.ok(gameService.createGame(game));
    }
}
