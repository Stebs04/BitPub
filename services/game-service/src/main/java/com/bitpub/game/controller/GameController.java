package com.bitpub.game.controller;

import com.bitpub.common.exception.ApiError;
import com.bitpub.game.model.Game;
import com.bitpub.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;

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

import com.bitpub.common.dto.PageResponse;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.common.specification.SearchOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.ArrayList;
import java.util.List;


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
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Errore interno del server.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<PageResponse<Game>> getAllGames(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
            
        List<SearchCriteria> criteria = new ArrayList<>();
        if (genre != null && !genre.isBlank()) {
            criteria.add(new SearchCriteria("genre", SearchOperation.EQUALITY, genre));
        }
        if (active != null) {
            criteria.add(new SearchCriteria("active", SearchOperation.EQUALITY, active));
        }
        
        return ResponseEntity.ok(gameService.getGames(criteria, pageable));
    }


}
