package com.bitpub.tournament.controller;

import com.bitpub.common.dto.ErrorResponse;
import com.bitpub.tournament.dto.CreateTournamentRequest;
import com.bitpub.tournament.dto.RegisterParticipantRequest;
import com.bitpub.tournament.dto.SubmitResultRequest;
import com.bitpub.tournament.model.Tournament;
import com.bitpub.tournament.model.TournamentMatch;
import com.bitpub.tournament.model.TournamentParticipant;
import com.bitpub.tournament.service.TournamentService;
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
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
@Tag(
    name = "Tournaments",
    description = """
        Gestione completa dei tornei sulla piattaforma BitPub.
        
        Il flusso standard è:
        1. Crea torneo → `POST /`
        2. Registra partecipanti → `POST /{id}/participants`
        3. Genera bracket → `POST /{id}/bracket/generate`
        4. Invia risultati incontri → `POST /matches/result`
        5. Leggi classifica finale → `GET /{id}/participants`
        """
)
@SecurityRequirement(name = "bearerAuth")
public class TournamentController {

    private final TournamentService tournamentService;

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Lista tutti i tornei",
        description = "Restituisce l'elenco di tutti i tornei presenti nel sistema, indipendentemente dallo stato."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista tornei restituita con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = Tournament.class)),
                examples = @ExampleObject(
                    name = "Lista tornei",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440001",
                            "name": "BitPub Spring Cup 2024",
                            "gameId": "550e8400-e29b-41d4-a716-446655440000",
                            "startDate": "2024-06-01T18:00:00",
                            "status": "OPEN"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante o non valido.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments/{id}
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Dettaglio torneo per ID",
        description = "Restituisce i dettagli completi di un singolo torneo tramite il suo UUID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Torneo trovato.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Tournament.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante o non valido.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Torneo non trovato con l'ID specificato.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "error": "Not Found",
                      "message": "Torneo non trovato con id: 550e8400-e29b-41d4-a716-000000000000",
                      "path": "/api/v1/tournaments/550e8400-e29b-41d4-a716-000000000000",
                      "timestamp": "2024-05-24T21:00:00"
                    }
                    """)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(
            @Parameter(description = "UUID del torneo", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Crea un nuovo torneo",
        description = """
            Crea un nuovo torneo nella piattaforma.
            
            **Richiede ruolo ADMIN.**
            
            Il torneo viene creato in stato `OPEN` e rimane aperto alle iscrizioni fino
            alla generazione del bracket.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Torneo creato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Tournament.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dati richiesta non validi.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Accesso negato: ruolo ADMIN richiesto.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Tournament> createTournament(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dati del torneo da creare.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = CreateTournamentRequest.class),
                    examples = @ExampleObject(
                        name = "Nuovo torneo",
                        value = """
                            {
                              "name": "BitPub Spring Cup 2024",
                              "gameId": "550e8400-e29b-41d4-a716-446655440000",
                              "startDate": "2024-06-01T18:00:00"
                            }
                            """
                    )
                )
            )
            @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.ok(tournamentService.createTournament(request));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments/{id}/participants
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Registra un partecipante al torneo",
        description = """
            Iscrive un utente come partecipante a un torneo aperto.
            
            - Verifica che il torneo sia in stato `OPEN`.
            - Evita doppie iscrizioni dello stesso utente.
            - Il partecipante viene aggiunto in stato `PENDING`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Partecipante registrato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TournamentParticipant.class),
                examples = @ExampleObject(value = """
                    {
                      "id": "7ba7b810-9dad-11d1-80b4-00c04fd430c8",
                      "tournamentId": "550e8400-e29b-41d4-a716-446655440001",
                      "userId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                      "username": "mario_rossi",
                      "status": "PENDING",
                      "score": 0
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Torneo non aperto o utente già iscritto.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Torneo non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/participants")
    public ResponseEntity<TournamentParticipant> registerParticipant(
            @Parameter(description = "UUID del torneo", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dati del partecipante da iscrivere.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = RegisterParticipantRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "userId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                          "username": "mario_rossi"
                        }
                        """)
                )
            )
            @RequestBody RegisterParticipantRequest request) {
        return ResponseEntity.ok(tournamentService.registerParticipant(id, request));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments/{id}/participants
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Classifica partecipanti del torneo",
        description = "Restituisce i partecipanti del torneo ordinati per punteggio (classifica)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Classifica restituita con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = TournamentParticipant.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Torneo non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<TournamentParticipant>> getParticipants(
            @Parameter(description = "UUID del torneo", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getLeaderboard(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments/{id}/bracket/generate
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Genera il bracket del torneo",
        description = """
            Genera il tabellone degli incontri per eliminazione diretta.
            
            - **Richiede ruolo ADMIN.**
            - Il torneo deve essere in stato `OPEN` con almeno 2 partecipanti.
            - Dopo la generazione, il torneo passa in stato `IN_PROGRESS`.
            - Può essere eseguito **una sola volta** per torneo.
            - Gli incontri vengono creati in stato `PENDING`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Bracket generato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = TournamentMatch.class)),
                examples = @ExampleObject(value = """
                    [
                      {
                        "id": "b3c1a2d0-9dad-11d1-80b4-00c04fd430c8",
                        "tournamentId": "550e8400-e29b-41d4-a716-446655440001",
                        "playerAId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                        "playerBId": "b4d3c2e1-f30c-52e5-b827-557766551113",
                        "status": "PENDING",
                        "round": 1
                      }
                    ]
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Torneo non in stato OPEN o partecipanti insufficienti.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Accesso negato: ruolo ADMIN richiesto.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Torneo non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/bracket/generate")
    public ResponseEntity<List<TournamentMatch>> generateBracket(
            @Parameter(description = "UUID del torneo", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.generateBracket(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments/{id}/bracket
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Recupera il bracket del torneo",
        description = "Restituisce tutti gli incontri del bracket del torneo, ordinati per round."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Bracket restituito con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = TournamentMatch.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Torneo non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/bracket")
    public ResponseEntity<List<TournamentMatch>> getBracket(
            @Parameter(description = "UUID del torneo", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getBracket(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments/matches/result
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Invia il risultato di un incontro",
        description = """
            Registra il risultato di un incontro del torneo.
            
            - Aggiorna i punteggi di `scoreA` e `scoreB`.
            - Determina il vincitore e aggiorna lo stato dell'incontro a `COMPLETED`.
            - Aggiorna automaticamente i punti del partecipante vincitore nella classifica.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Risultato registrato con successo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TournamentMatch.class),
                examples = @ExampleObject(value = """
                    {
                      "id": "b3c1a2d0-9dad-11d1-80b4-00c04fd430c8",
                      "tournamentId": "550e8400-e29b-41d4-a716-446655440001",
                      "playerAId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                      "playerBId": "b4d3c2e1-f30c-52e5-b827-557766551113",
                      "scoreA": 3,
                      "scoreB": 1,
                      "winnerId": "a3c2b1d0-e29b-41d4-a716-446655440002",
                      "status": "COMPLETED",
                      "round": 1
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Incontro non in stato PENDING o punteggi non validi.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "JWT mancante.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Incontro non trovato.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/matches/result")
    public ResponseEntity<TournamentMatch> submitResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dati del risultato dell'incontro.",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = SubmitResultRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "matchId": "b3c1a2d0-9dad-11d1-80b4-00c04fd430c8",
                          "scoreA": 3,
                          "scoreB": 1
                        }
                        """)
                )
            )
            @RequestBody SubmitResultRequest request) {
        return ResponseEntity.ok(tournamentService.submitResult(request));
    }
}
