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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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

    @Value("${statistics.service.url:http://localhost:8087}")
    private String statisticsServiceUrl;

    /**
     * La classifica del torneo si calcola retroattivamente dagli scontri gia' giocati del tabellone
     * (winnerId valorizzato): sorgente auto-contenuta, senza dipendere da match per nome sulla
     * leaderboard globale. Ricalcolata a ogni lettura, quindi recupera anche le partite vecchie.
     * Ordinata per vittorie; i gol segnati arrivano dalla leaderboard globale del gioco del torneo.
     */
    @Override
    @Transactional
    public List<TournamentRankingDto> getTournamentRankings(String tournamentId) {
        syncFromBracket(tournamentId);
        String gameTypeId = tournamentRepository.findById(tournamentId)
                .map(Tournament::getGameTypeId).orElse(null);
        Map<String, Integer> goalsByPlayer = fetchGoals(gameTypeId);
        return rankingRepository.findByTournamentIdOrderByMatchesWonDesc(tournamentId).stream()
                .map(r -> mapToDto(r, goalsByPlayer.getOrDefault(
                        r.getParticipantName() == null ? "" : r.getParticipantName().toLowerCase(), 0)))
                .collect(Collectors.toList());
    }

    /** playerName (lowercase) -> gol totali, dalla leaderboard globale del gioco. Best-effort. */
    private Map<String, Integer> fetchGoals(String gameTypeId) {
        Map<String, Integer> goals = new HashMap<>();
        if (gameTypeId == null) return goals;
        try {
            List<Map<String, Object>> entries = RestClient.create(statisticsServiceUrl)
                    .get().uri("/api/v1/statistics/leaderboard/{gameTypeId}", gameTypeId)
                    .retrieve().body(List.class);
            if (entries != null) {
                for (Map<String, Object> e : entries) {
                    Object name = e.get("playerName");
                    Object pts = e.get("totalPoints");
                    if (name != null && pts instanceof Number n) {
                        goals.put(name.toString().toLowerCase(), n.intValue());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Fetch gol leaderboard per gioco {} fallito", gameTypeId, ex);
        }
        return goals;
    }

    private void syncFromBracket(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament == null) return;
        List<TournamentRanking> rankings = rankingRepository.findByTournamentIdOrderByScoreDesc(tournamentId);
        if (rankings.isEmpty()) return;

        // participantId -> [partite giocate, vittorie]. Conta ogni scontro concluso del tabellone.
        Map<String, int[]> tally = new HashMap<>();
        List<TournamentMatch> bracket = tournament.getBracketMatches();
        if (bracket != null) {
            for (TournamentMatch m : bracket) {
                if (m.getWinnerId() == null) continue; // scontro non ancora giocato
                countPlayed(tally, m.getPlayer1Id());
                countPlayed(tally, m.getPlayer2Id());
                tally.computeIfAbsent(m.getWinnerId(), k -> new int[2])[1]++;
            }
        }

        for (TournamentRanking r : rankings) {
            int[] t = tally.getOrDefault(r.getParticipantId(), new int[2]);
            r.setMatchesPlayed(t[0]);
            r.setMatchesWon(t[1]);
        }
        rankingRepository.saveAll(rankings);
        recalculateRankings(tournamentId);
    }

    private void countPlayed(Map<String, int[]> tally, String participantId) {
        if (participantId != null) tally.computeIfAbsent(participantId, k -> new int[2])[0]++;
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
        // ponytail: endpoint legacy (non usato dalla WebApp); gol non risolti qui, li calcola la lettura classifica.
        return mapToDto(ranking, 0);
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

    private TournamentRankingDto mapToDto(TournamentRanking ranking, int goalsScored) {
        return TournamentRankingDto.builder()
                .id(ranking.getId())
                .tournamentId(ranking.getTournament().getId())
                .participantId(ranking.getParticipantId())
                .participantName(ranking.getParticipantName())
                .goalsScored(goalsScored)
                .matchesPlayed(ranking.getMatchesPlayed())
                .matchesWon(ranking.getMatchesWon())
                .currentRank(ranking.getCurrentRank())
                .build();
    }
}
