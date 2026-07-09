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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentRankingServiceImplTest {

    @Mock private TournamentRankingRepository rankingRepository;
    @Mock private TournamentRegistrationRepository registrationRepository;
    @Mock private TournamentRepository tournamentRepository;

    @InjectMocks private TournamentRankingServiceImpl service;

    private Tournament tournament(String id) {
        return Tournament.builder().id(id).name("T").status("ACTIVE").bracketMatches(new ArrayList<>()).build();
    }

    private TournamentRanking ranking(Tournament t, String pid, String name) {
        return TournamentRanking.builder().tournament(t).participantId(pid).participantName(name)
                .score(0).matchesPlayed(0).matchesWon(0).currentRank(0).build();
    }

    @Test
    void updateRankingScore_win_incrementsScorePlayedAndWins() {
        Tournament t = tournament("t1");
        TournamentRanking r = ranking(t, "a", "A");
        when(rankingRepository.findByTournamentIdAndParticipantId("t1", "a")).thenReturn(Optional.of(r));
        when(rankingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rankingRepository.findByTournamentIdOrderByMatchesWonDesc("t1")).thenReturn(List.of(r));

        TournamentRankingDto dto = service.updateRankingScore("t1", "a", 3, true);

        assertThat(dto.getMatchesPlayed()).isEqualTo(1);
        assertThat(dto.getMatchesWon()).isEqualTo(1);
        assertThat(r.getScore()).isEqualTo(3);
    }

    @Test
    void updateRankingScore_participantNotRanked_throws() {
        when(rankingRepository.findByTournamentIdAndParticipantId("t1", "x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRankingScore("t1", "x", 1, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void initializeRankings_createsOneRankingPerRegistration() {
        Tournament t = tournament("t1");
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));
        when(registrationRepository.findByTournamentId("t1"))
                .thenReturn(List.of(
                        TournamentRegistration.builder().participantId("a").participantName("A").build(),
                        TournamentRegistration.builder().participantId("b").participantName("B").build()));
        when(rankingRepository.findByTournamentIdAndParticipantId(eq("t1"), any())).thenReturn(Optional.empty());
        when(rankingRepository.findByTournamentIdOrderByMatchesWonDesc("t1")).thenReturn(List.of());

        service.initializeRankingsForTournament("t1");

        verify(rankingRepository, times(2)).save(any()); // una riga per iscritto
    }

    // ── getTournamentRankings: la classifica si ricostruisce dai vincitori del tabellone ──
    @Test
    void getTournamentRankings_countsWinsFromBracket() {
        Tournament t = tournament("t1");
        TournamentMatch played = TournamentMatch.builder().id("m0").tournament(t).round(0).matchIndex(0)
                .player1Id("a").player2Id("b").player1Goals(2).player2Goals(1).winnerId("a").build();
        t.getBracketMatches().add(played);

        TournamentRanking ra = ranking(t, "a", "A");
        TournamentRanking rb = ranking(t, "b", "B");
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));
        when(rankingRepository.findByTournamentIdOrderByScoreDesc("t1")).thenReturn(List.of(ra, rb));
        when(rankingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rankingRepository.findByTournamentIdOrderByMatchesWonDesc("t1")).thenReturn(List.of(ra, rb));

        List<TournamentRankingDto> result = service.getTournamentRankings("t1");

        assertThat(result).extracting(TournamentRankingDto::getParticipantId, TournamentRankingDto::getMatchesWon)
                .contains(org.assertj.core.groups.Tuple.tuple("a", 1), org.assertj.core.groups.Tuple.tuple("b", 0));
        assertThat(ra.getGoalsScored()).isEqualTo(2);
    }
}
