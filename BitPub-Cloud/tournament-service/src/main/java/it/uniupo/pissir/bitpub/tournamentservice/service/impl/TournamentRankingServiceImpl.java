package it.uniupo.pissir.bitpub.tournamentservice.service.impl;

import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentMatch;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRanking;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRankingRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRegistrationRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRepository;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentRankingServiceImpl implements TournamentRankingService {

    private final TournamentRankingRepository rankingRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;

    /**
     * La classifica del torneo si calcola retroattivamente dagli scontri gia' giocati del tabellone
     * (winnerId valorizzato): sorgente auto-contenuta. Ordinata per vittorie. I gol segnati sono quelli
     * ingeriti via MQTT sugli scontri del tabellone (player1Goals/player2Goals), ISOLATI dalla
     * leaderboard globale: contano solo le partite di questo torneo. Ricalcolata a ogni lettura.
     */
    @Override
    @Transactional
    public List<TournamentRankingDto> getTournamentRankings(String tournamentId) {
        syncFromBracket(tournamentId);
        return rankingRepository.findByTournamentIdOrderByMatchesWonDesc(tournamentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void syncFromBracket(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament == null) return;
        List<TournamentRanking> rankings = rankingRepository.findByTournamentIdOrderByScoreDesc(tournamentId);
        if (rankings.isEmpty()) return;

        // participantId -> [partite giocate, vittorie, gol]. Conta ogni scontro concluso del tabellone;
        // i gol arrivano dagli eventi MQTT match-result (isolati a questo torneo).
        Map<String, int[]> tally = new HashMap<>();
        List<TournamentMatch> bracket = tournament.getBracketMatches();
        if (bracket != null) {
            for (TournamentMatch m : bracket) {
                if (m.getWinnerId() == null) continue; // scontro non ancora giocato
                addTally(tally, m.getPlayer1Id(), m.getPlayer1Goals(), m.getWinnerId());
                addTally(tally, m.getPlayer2Id(), m.getPlayer2Goals(), m.getWinnerId());
            }
        }

        for (TournamentRanking r : rankings) {
            int[] t = tally.getOrDefault(r.getParticipantId(), new int[3]);
            r.setMatchesPlayed(t[0]);
            r.setMatchesWon(t[1]);
            r.setGoalsScored(t[2]);
        }
        rankingRepository.saveAll(rankings);
        recalculateRankings(tournamentId);
    }

    private void addTally(Map<String, int[]> tally, String participantId, int goals, String winnerId) {
        if (participantId == null) return;
        int[] t = tally.computeIfAbsent(participantId, k -> new int[3]);
        t[0]++;             // partita giocata
        t[2] += goals;      // gol nel torneo
        if (participantId.equals(winnerId)) t[1]++; // vittoria
    }

    @Override
    @Transactional
    public TournamentRankingDto updateRankingScore(String tournamentId, String participantId, int scoreDelta, boolean isWin) {
        TournamentRanking ranking = rankingRepository.findByTournamentIdAndParticipantId(tournamentId, participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking not found for participant " + participantId + " in tournament " + tournamentId));
        
        ranking.setScore(ranking.getScore() + scoreDelta);
        ranking.setMatchesPlayed(ranking.getMatchesPlayed() + 1);
        if (isWin) {
            ranking.setMatchesWon(ranking.getMatchesWon() + 1);
        }
        
        ranking = rankingRepository.save(ranking);
        recalculateRankings(tournamentId);
        return mapToDto(ranking);
    }

    @Override
    @Transactional
    public void initializeRankingsForTournament(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));
        
        List<TournamentRegistration> registrations = registrationRepository.findByTournamentId(tournamentId);
        for (TournamentRegistration reg : registrations) {
            if (rankingRepository.findByTournamentIdAndParticipantId(tournamentId, reg.getParticipantId()).isEmpty()) {
                TournamentRanking ranking = TournamentRanking.builder()
                        .tournament(tournament)
                        .participantId(reg.getParticipantId())
                        .participantName(reg.getParticipantName())
                        .score(0)
                        .matchesPlayed(0)
                        .matchesWon(0)
                        .currentRank(0)
                        .build();
                rankingRepository.save(ranking);
            }
        }
        recalculateRankings(tournamentId);
    }

    private void recalculateRankings(String tournamentId) {
        List<TournamentRanking> rankings = rankingRepository.findByTournamentIdOrderByMatchesWonDesc(tournamentId);
        int currentRank = 1;
        for (TournamentRanking ranking : rankings) {
            ranking.setCurrentRank(currentRank++);
            rankingRepository.save(ranking);
        }
    }

    private TournamentRankingDto mapToDto(TournamentRanking ranking) {
        return TournamentRankingDto.builder()
                .id(ranking.getId())
                .tournamentId(ranking.getTournament().getId())
                .participantId(ranking.getParticipantId())
                .participantName(ranking.getParticipantName())
                .goalsScored(ranking.getGoalsScored())
                .matchesPlayed(ranking.getMatchesPlayed())
                .matchesWon(ranking.getMatchesWon())
                .currentRank(ranking.getCurrentRank())
                .build();
    }
}
