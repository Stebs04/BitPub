package com.bitpub.stats.controller;

import com.bitpub.stats.dto.LeaderboardEntryDto;
import com.bitpub.common.dto.PageResponse;
import com.bitpub.stats.dto.RecordMatchRequest;
import com.bitpub.stats.model.MatchResult;
import com.bitpub.stats.model.PlayerStats;
import com.bitpub.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bitpub.common.security.enums.Role;
import com.bitpub.common.security.annotations.RequireRole;
import com.bitpub.common.security.annotations.RequireOwnership;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.common.specification.SearchOperation;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(
    name = "Statistics",
    description = "API di statistiche e classifiche della piattaforma BitPub con paginazione."
)
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Registra il risultato di una partita")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Risultato registrato con successo."),
        @ApiResponse(responseCode = "400", description = "Dati della richiesta non validi."),
        @ApiResponse(responseCode = "401", description = "JWT mancante o non valido.")
    })
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLAYER')")
    @RequireRole({Role.PLATFORM_ADMIN, Role.PLAYER})
    @PostMapping("/matches")
    public ResponseEntity<MatchResult> recordMatch(@jakarta.validation.Valid @RequestBody RecordMatchRequest request) {
        return ResponseEntity.ok(statsService.recordMatch(request));
    }

    @Operation(summary = "Leaderboard globale")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaderboard globale restituita."),
        @ApiResponse(responseCode = "401", description = "JWT mancante.")
    })
    @GetMapping("/leaderboard/global")
    public ResponseEntity<PageResponse<LeaderboardEntryDto>> getGlobalLeaderboard(
            @Parameter(description = "Parametri di paginazione") @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(statsService.getGlobalLeaderboard(pageable));
    }

    @Operation(summary = "Leaderboard per gioco specifico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaderboard del gioco restituita."),
        @ApiResponse(responseCode = "401", description = "JWT mancante."),
        @ApiResponse(responseCode = "404", description = "Gioco non trovato.")
    })
    @GetMapping("/leaderboard/game/{gameId}")
    public ResponseEntity<PageResponse<LeaderboardEntryDto>> getLeaderboardByGame(
            @PathVariable UUID gameId,
            @Parameter(description = "Parametri di paginazione") @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(statsService.getLeaderboardByGame(gameId, pageable));
    }

    @Operation(summary = "Storico partite di un utente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Storico partite restituito."),
        @ApiResponse(responseCode = "401", description = "JWT mancante."),
        @ApiResponse(responseCode = "404", description = "Utente non trovato.")
    })
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'LOCAL_ADMIN', 'GAME_ADMIN') or #userId.toString() == authentication.principal.userId")
    @RequireOwnership
    @GetMapping("/history/{userId}")
    public ResponseEntity<PageResponse<MatchResult>> getMatchHistory(
            @PathVariable UUID userId,
            @Parameter(description = "Parametri di paginazione") @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(statsService.getMatchHistory(userId, pageable));
    }

    @Operation(summary = "Statistiche giocatore per gioco")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiche giocatore restituite."),
        @ApiResponse(responseCode = "401", description = "JWT mancante."),
        @ApiResponse(responseCode = "404", description = "Statistiche non trovate.")
    })
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'LOCAL_ADMIN', 'GAME_ADMIN') or #userId.toString() == authentication.principal.userId")
    @RequireOwnership
    @GetMapping("/players/{userId}/game/{gameId}")
    public ResponseEntity<PlayerStats> getPlayerStats(
            @PathVariable UUID userId,
            @PathVariable UUID gameId) {
        return ResponseEntity.ok(statsService.getPlayerStats(userId, gameId));
    }
    
    @Operation(summary = "Cerca statistiche avanzate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'LOCAL_ADMIN', 'GAME_ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<PlayerStats>> getStats(
            @RequestParam(required = false) UUID gameId,
            @PageableDefault(size = 20) Pageable pageable) {
            
        List<SearchCriteria> criteria = new ArrayList<>();
        if (gameId != null) {
            criteria.add(new SearchCriteria("gameId", SearchOperation.EQUALITY, gameId));
        }
        
        return ResponseEntity.ok(statsService.getStats(criteria, pageable));
    }
}
