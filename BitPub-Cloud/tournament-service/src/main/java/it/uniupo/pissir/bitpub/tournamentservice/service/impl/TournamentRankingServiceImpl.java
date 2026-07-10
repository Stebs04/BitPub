/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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

/**
 * Implementazione concreta del servizio di gestione delle classifiche per i tornei.
 * Ricalcola i punteggi in modo dinamico a partire dai risultati presenti nel tabellone.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentRankingServiceImpl implements TournamentRankingService {

    private final TournamentRankingRepository rankingRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;

    /**
     * Recupera la classifica ricalcolandola retroattivamente basandosi sulle partite concluse nel tabellone
     * (dove è stato stabilito un vincitore). La classifica è locale per il torneo ed è ordinata in base alle vittorie.
     * I gol considerati provengono esclusivamente dagli eventi MQTT relativi alle partite del torneo corrente, 
     * isolandoli in questo modo dalle statistiche e dalle leaderboard globali. 
     * L'aggiornamento avviene dinamicamente a ogni richiesta di lettura.
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

        // Mappa participantId -> [partite giocate, vittorie, gol nel torneo]. 
        // Calcola i totali scandendo tutti gli scontri conclusi; i gol arrivano dagli eventi MQTT del match-result.
        Map<String, int[]> tally = new HashMap<>();
        List<TournamentMatch> bracket = tournament.getBracketMatches();
        if (bracket != null) {
            for (TournamentMatch m : bracket) {
                if (m.getWinnerId() == null) continue; // Salta le partite non ancora concluse
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
        t[0]++;             // Incrementa contatore partite giocate
        t[2] += goals;      // Aggiorna totale gol segnati nel torneo
        if (participantId.equals(winnerId)) t[1]++; // Se è il vincitore, incrementa partite vinte
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
