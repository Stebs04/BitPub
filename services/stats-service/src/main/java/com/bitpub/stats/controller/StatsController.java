package com.bitpub.stats.controller;

import com.bitpub.common.dto.ErrorResponse;
import com.bitpub.stats.dto.LeaderboardEntryDto;
import com.bitpub.stats.dto.RecordMatchRequest;
import com.bitpub.stats.model.MatchResult;
import com.bitpub.stats.model.PlayerStats;
import com.bitpub.stats.service.StatsService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(
    name = "Statistics",
    description = """
        API di statistiche e classifiche della piattaforma BitPub.
        
        - `POST /matches` è chiamato internamente da **Edge-Sync Service** o **Game Service**.
          È idempotente: la stessa `matchSessionId` non produce duplicati.
        - Le leaderboard sono calcolate in tempo reale dalle statistiche aggregate per giocatore.
        """
)
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    // -------------------------------------------------------------------------
    // POST /api/v1/stats/matches
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Registra il risultato di una partita",
        description = """
            Registra il risultato di una partita completata e aggiorna le statistiche dei giocatori.
            
            **Idempotente**: la stessa `matchSessionId` non viene processata due volte.
            Questo endpoint è tipicamente chiamato dall'**Edge-Sync Service** o dal **Game Service**,
            non direttamente dal client frontend.
            
            Aggiorna atomicamente:
            - `PlayerStats` del vincitore (wins++, totalScore += winnerScore)
            - `PlayerStats` del perdente (losses++, totalScore += loserScore)
            - `Leaderboard` globale e per gioco
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Risultato registrato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = MatchResult.class),
                examples = @ExampleObject(
                    name = "Partita registrata",
                    value = """
                        {
                          "id": "c1d2e3f4-a5b6-41d4-a716-446655440001",
                          "matchSessionId": "d4e5f6a7-b8c9-41d4-a716-446655440002",
                          "gameId": "550e8400-e29b-41d4-a716-446655440000",
                          "winnerUserId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                          "winnerUsername": "mario_rossi",
                          "loserUserId": "b4d3c2e1-f30c-52e5-b827-557766551113",
                          "loserUsername": "luigi_verdi",
                          "winnerScore": 5,
                          "loserScore": 2,
                          "recordedAt": "2024-05-24T21:00:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Partita già registrata (idempotenza): restituisce il record esistente."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dati della richiesta non validi.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante o non valido.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/matches")
    public ResponseEntity<MatchResult> recordMatch(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dati completi della partita terminata.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = RecordMatchRequest.class),
                    examples = @ExampleObject(
                        name = "Risultato partita",
                        value = """
                            {
                              "matchSessionId": "d4e5f6a7-b8c9-41d4-a716-446655440002",
                              "gameId": "550e8400-e29b-41d4-a716-446655440000",
                              "winnerUserId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                              "winnerUsername": "mario_rossi",
                              "loserUserId": "b4d3c2e1-f30c-52e5-b827-557766551113",
                              "loserUsername": "luigi_verdi",
                              "winnerScore": 5,
                              "loserScore": 2
                            }
                            """
                    )
                )
            )
            @RequestBody RecordMatchRequest request) {
        return ResponseEntity.ok(statsService.recordMatch(request));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/stats/leaderboard/global
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Leaderboard globale",
        description = """
            Restituisce la classifica globale di tutti i giocatori della piattaforma,
            ordinata per numero di **vittorie** in modo decrescente.
            
            Include tutti i giochi. Il campo `winRate` è calcolato come `wins / totalMatches`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Leaderboard globale restituita.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = LeaderboardEntryDto.class)),
                examples = @ExampleObject(
                    name = "Leaderboard globale",
                    value = """
                        [
                          {
                            "rank": 1,
                            "userId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                            "username": "mario_rossi",
                            "wins": 42,
                            "losses": 8,
                            "totalMatches": 50,
                            "totalScore": 210,
                            "winRate": 0.84
                          },
                          {
                            "rank": 2,
                            "userId": "b4d3c2e1-f30c-52e5-b827-557766551113",
                            "username": "luigi_verdi",
                            "wins": 38,
                            "losses": 12,
                            "totalMatches": 50,
                            "totalScore": 190,
                            "winRate": 0.76
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/leaderboard/global")
    public ResponseEntity<List<LeaderboardEntryDto>> getGlobalLeaderboard() {
        return ResponseEntity.ok(statsService.getGlobalLeaderboard());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/stats/leaderboard/game/{gameId}
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Leaderboard per gioco specifico",
        description = """
            Restituisce la classifica dei giocatori filtrata per un singolo gioco,
            ordinata per vittorie nel gioco selezionato.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Leaderboard del gioco restituita.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = LeaderboardEntryDto.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Gioco non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/leaderboard/game/{gameId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboardByGame(
            @Parameter(
                description = "UUID del gioco per cui filtrare la classifica.",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathVariable UUID gameId) {
        return ResponseEntity.ok(statsService.getLeaderboardByGame(gameId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/stats/history/{userId}
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Storico partite di un utente",
        description = """
            Restituisce lo storico completo delle partite giocate da un utente specifico,
            ordinate dalla più recente alla più vecchia.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Storico partite restituito.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = MatchResult.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Utente non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<MatchResult>> getMatchHistory(
            @Parameter(
                description = "UUID dell'utente di cui recuperare lo storico.",
                example = "a3c2b1d0-e29b-41d4-a716-446655440002",
                required = true
            )
            @PathVariable UUID userId) {
        return ResponseEntity.ok(statsService.getMatchHistory(userId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/stats/players/{userId}/game/{gameId}
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Statistiche giocatore per gioco",
        description = """
            Restituisce le statistiche dettagliate di un singolo giocatore per un gioco specifico:
            vittorie, sconfitte, partite totali, punteggio totale e win rate.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Statistiche giocatore restituite.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PlayerStats.class),
                examples = @ExampleObject(
                    name = "Statistiche giocatore",
                    value = """
                        {
                          "id": "e5f6a7b8-c9d0-41d4-a716-446655440003",
                          "userId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                          "gameId": "550e8400-e29b-41d4-a716-446655440000",
                          "wins": 15,
                          "losses": 3,
                          "totalMatches": 18,
                          "totalScore": 75
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Statistiche non trovate per la combinazione utente/gioco.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/players/{userId}/game/{gameId}")
    public ResponseEntity<PlayerStats> getPlayerStats(
            @Parameter(
                description = "UUID dell'utente.",
                example = "a3c2b1d0-e29b-41d4-a716-446655440002",
                required = true
            )
            @PathVariable UUID userId,
            @Parameter(
                description = "UUID del gioco.",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
            )
            @PathVariable UUID gameId) {
        return ResponseEntity.ok(statsService.getPlayerStats(userId, gameId));
    }
}
