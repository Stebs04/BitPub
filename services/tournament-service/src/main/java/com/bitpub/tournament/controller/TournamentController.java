package com.bitpub.tournament.controller;

import com.bitpub.common.exception.ApiError;
import com.bitpub.tournament.dto.*;
import com.bitpub.tournament.mapper.TournamentMapper;
import com.bitpub.tournament.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Gestione completa dei tornei sulla piattaforma BitPub.")
@SecurityRequirement(name = "bearerAuth")
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentMapper mapper;

    @Operation(summary = "Lista tutti i tornei")
    @GetMapping
    public ResponseEntity<PageResponse<TournamentDto>> getAllTournaments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        List<SearchCriteria> criteria = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            criteria.add(new SearchCriteria("status", SearchOperation.EQUALITY, status));
        }

        PageResponse<com.bitpub.tournament.model.Tournament> page = tournamentService.getTournaments(criteria, pageable);
        PageResponse<TournamentDto> dtoPage = new PageResponse<>();
        dtoPage.setContent(page.getContent().stream().map(mapper::toDto).collect(Collectors.toList()));
        dtoPage.setPageNumber(page.getPageNumber());
        dtoPage.setPageSize(page.getPageSize());
        dtoPage.setTotalElements(page.getTotalElements());
        dtoPage.setTotalPages(page.getTotalPages());
        dtoPage.setLast(page.isLast());

        return ResponseEntity.ok(dtoPage);
    }

    @Operation(summary = "Dettaglio torneo per ID")
    @GetMapping("/{id}")
    public ResponseEntity<TournamentDto> getTournamentById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toDto(tournamentService.getTournamentById(id)));
    }

    @Operation(summary = "Crea un nuovo torneo")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @RequireRole(Role.PLATFORM_ADMIN)
    @PostMapping
    public ResponseEntity<TournamentDto> createTournament(@Valid @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.ok(mapper.toDto(tournamentService.createTournament(request)));
    }

    @Operation(summary = "Registra un team o giocatore al torneo")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/teams")
    public ResponseEntity<TeamDto> registerTeam(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterTeamRequest request) {
        return ResponseEntity.ok(mapper.toDto(tournamentService.registerTeam(id, request)));
    }

    @Operation(summary = "Genera il bracket o calendario del torneo")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @RequireRole(Role.PLATFORM_ADMIN)
    @PostMapping("/{id}/bracket/generate")
    public ResponseEntity<List<TournamentMatchDto>> generateBracket(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.generateBracket(id)
                .stream().map(mapper::toDto).collect(Collectors.toList()));
    }

    @Operation(summary = "Recupera i match del torneo")
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<TournamentMatchDto>> getMatches(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getMatches(id)
                .stream().map(mapper::toDto).collect(Collectors.toList()));
    }

    @Operation(summary = "Invia il risultato di un incontro")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @RequireRole(Role.PLATFORM_ADMIN)
    @PostMapping("/matches/result")
    public ResponseEntity<TournamentMatchDto> submitResult(@Valid @RequestBody SubmitResultRequest request) {
        return ResponseEntity.ok(mapper.toDto(tournamentService.submitResult(request)));
    }

    @Operation(summary = "Classifica (Leaderboard) del torneo")
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getLeaderboard(id)
                .stream().map(mapper::toDto).collect(Collectors.toList()));
    }
}
