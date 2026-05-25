package com.bitpub.tournament.service;

import com.bitpub.tournament.dto.CreateTournamentRequest;
import com.bitpub.tournament.dto.RegisterParticipantRequest;
import com.bitpub.tournament.dto.SubmitResultRequest;
import com.bitpub.tournament.model.*;
import com.bitpub.tournament.repository.TournamentMatchRepository;
import com.bitpub.tournament.repository.TournamentParticipantRepository;
import com.bitpub.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository matchRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Tournament getTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found: " + id));
    }

    @Transactional
    public Tournament createTournament(CreateTournamentRequest request) {
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .gameId(UUID.fromString(request.getGameId()))
                .status("REGISTRATION")
                .startDate(request.getStartDate())
                .build();
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public TournamentParticipant registerParticipant(UUID tournamentId, RegisterParticipantRequest request) {
        Tournament tournament = getTournamentById(tournamentId);
        if (!"REGISTRATION".equals(tournament.getStatus())) {
            throw new IllegalStateException("Tournament is not open for registration");
        }
        if (participantRepository.existsByTournamentIdAndUserId(tournamentId, request.getUserId())) {
            throw new IllegalStateException("User already registered in this tournament");
        }

        long currentCount = participantRepository.findByTournamentId(tournamentId).size();
        TournamentParticipant participant = TournamentParticipant.builder()
                .tournament(tournament)
                .userId(request.getUserId())
                .username(request.getUsername())
                .seed((int) currentCount + 1)
                .build();
        return participantRepository.save(participant);
    }

    /**
     * Genera il bracket a eliminazione singola.
     * Il numero di partecipanti viene arrotondato alla potenza di 2 superiore (es: 5 → 8 slot con BYE).
     */
    @Transactional
    public List<TournamentMatch> generateBracket(UUID tournamentId) {
        Tournament tournament = getTournamentById(tournamentId);
        List<TournamentParticipant> participants = participantRepository.findByTournamentId(tournamentId);

        if (participants.size() < 2) {
            throw new IllegalStateException("At least 2 participants required to generate bracket");
        }

        Collections.shuffle(participants); // seed random (può essere ordinato per ELO in futuro)

        int slots = nextPowerOfTwo(participants.size());
        List<TournamentMatch> matches = new ArrayList<>();

        // Round 1: accoppia i partecipanti
        for (int i = 0; i < slots / 2; i++) {
            UUID playerA = (i * 2 < participants.size()) ? participants.get(i * 2).getUserId() : null;
            UUID playerB = (i * 2 + 1 < participants.size()) ? participants.get(i * 2 + 1).getUserId() : null;

            TournamentMatch match = TournamentMatch.builder()
                    .tournament(tournament)
                    .round(1)
                    .matchIndex(i)
                    .playerAId(playerA)
                    .playerBId(playerB)
                    // Se uno slot è BYE (null), il giocatore presente avanza automaticamente
                    .status(playerB == null ? MatchStatus.FINISHED : MatchStatus.SCHEDULED)
                    .winnerId(playerB == null ? playerA : null)
                    .build();
            matches.add(matchRepository.save(match));
        }

        // Crea slot vuoti per i round successivi (2, 3, ...)
        int totalRounds = (int) (Math.log(slots) / Math.log(2));
        for (int round = 2; round <= totalRounds; round++) {
            int matchesInRound = slots / (int) Math.pow(2, round);
            for (int i = 0; i < matchesInRound; i++) {
                TournamentMatch match = TournamentMatch.builder()
                        .tournament(tournament)
                        .round(round)
                        .matchIndex(i)
                        .status(MatchStatus.SCHEDULED)
                        .build();
                matches.add(matchRepository.save(match));
            }
        }

        tournament.setStatus("ONGOING");
        tournamentRepository.save(tournament);

        return matches;
    }

    /**
     * Registra il risultato di un match e avanza il vincitore al prossimo round.
     */
    @Transactional
    public TournamentMatch submitResult(SubmitResultRequest request) {
        TournamentMatch match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalStateException("Match already finished");
        }

        match.setScoreA(request.getScoreA());
        match.setScoreB(request.getScoreB());

        UUID winner = request.getScoreA() >= request.getScoreB() ? match.getPlayerAId() : match.getPlayerBId();
        match.setWinnerId(winner);
        match.setStatus(MatchStatus.FINISHED);
        matchRepository.save(match);

        // Avanza il vincitore al match del round successivo
        advanceWinner(match, winner);

        return match;
    }

    public List<TournamentMatch> getBracket(UUID tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundAscMatchIndexAsc(tournamentId);
    }

    public List<TournamentParticipant> getLeaderboard(UUID tournamentId) {
        return participantRepository.findByTournamentId(tournamentId);
    }

    // ---- Private helpers ----

    private void advanceWinner(TournamentMatch finishedMatch, UUID winnerId) {
        List<TournamentMatch> nextRoundMatches = matchRepository.findByTournamentIdAndRound(
                finishedMatch.getTournament().getId(), finishedMatch.getRound() + 1);

        if (nextRoundMatches.isEmpty()) {
            // Nessun round successivo: il torneo è finito
            finishedMatch.getTournament().setStatus("FINISHED");
            tournamentRepository.save(finishedMatch.getTournament());
            return;
        }

        int nextMatchIndex = finishedMatch.getMatchIndex() / 2;
        TournamentMatch nextMatch = nextRoundMatches.stream()
                .filter(m -> m.getMatchIndex() == nextMatchIndex)
                .findFirst()
                .orElseThrow();

        // Posiziona il vincitore nello slot corretto (A o B)
        if (finishedMatch.getMatchIndex() % 2 == 0) {
            nextMatch.setPlayerAId(winnerId);
        } else {
            nextMatch.setPlayerBId(winnerId);
        }
        matchRepository.save(nextMatch);
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }
}
