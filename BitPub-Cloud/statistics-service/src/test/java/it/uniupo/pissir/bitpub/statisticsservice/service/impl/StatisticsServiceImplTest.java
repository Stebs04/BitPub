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

        assert processed == 2; // conteggio = eventi ricevuti, incluso quello di torneo
        verify(leaderboardRepository).deleteAll();
        // solo il match non-torneo scrive la classifica (1 save: vincitore, nessun loser)
        verify(leaderboardRepository, times(1)).save(any(Leaderboard.class));
        // un solo broadcast: gameTypeId "chess" distinto
        verify(mqttOutboundChannel, times(1)).send(any());
    }
}
