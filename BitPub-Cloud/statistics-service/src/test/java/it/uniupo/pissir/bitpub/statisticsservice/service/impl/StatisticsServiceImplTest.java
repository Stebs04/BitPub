/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.repository.AggregateStatisticRepository;
import it.uniupo.pissir.bitpub.statisticsservice.repository.LeaderboardRepository;
import it.uniupo.pissir.bitpub.statisticsservice.repository.MatchHistoryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite del core logico per la generazione e aggiornamento statistiche.
 * Comprende scenari per l'idempotenza e l'isolamento dei dati legati ai tornei.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock LeaderboardRepository leaderboardRepository;
    @Mock AggregateStatisticRepository statisticRepository;
    @Mock MatchHistoryRecordRepository historyRepository;
    @Mock MessageChannel mqttOutboundChannel;

    StatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StatisticsServiceImpl(
                statisticRepository, leaderboardRepository, historyRepository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "mqttOutboundChannel", mqttOutboundChannel);
    }

    @Test
    void rebuildLeaderboard_excludesTournamentMatches_and_broadcastsOncePerGame() {
        when(leaderboardRepository.findByPlayerNameIgnoreCaseAndGameTypeId(any(), any()))
                .thenReturn(java.util.Optional.empty());
        when(leaderboardRepository.findByGameTypeIdOrderByWinsDescTotalPointsDesc(any()))
                .thenReturn(List.of());

        MatchResultEvent normal = MatchResultEvent.builder()
                .gameTypeId("chess").winnerName("Alice").winnerScore(10).build();
        MatchResultEvent tournament = MatchResultEvent.builder()
                .gameTypeId("chess").winnerName("Bob").winnerScore(9)
                .tournamentMatchId("tm-1").build();

        int processed = service.rebuildLeaderboard(List.of(normal, tournament));

        assert processed == 2; // Verifica che entrambi gli eventi siano stati processati
        verify(leaderboardRepository).deleteAll();
        // Assicura che solo l'evento relativo a un match non di torneo incida sulla classifica
        verify(leaderboardRepository, times(1)).save(any(Leaderboard.class));
        // Controlla che venga emesso esattamente un solo broadcast per la singola tipologia di gioco
        verify(mqttOutboundChannel, times(1)).send(any());
    }
}
