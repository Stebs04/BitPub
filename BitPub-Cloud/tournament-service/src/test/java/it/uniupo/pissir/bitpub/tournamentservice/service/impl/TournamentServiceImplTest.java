package it.uniupo.pissir.bitpub.tournamentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentMatch;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentMatchDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TeamRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentMatchRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRankingRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRegistrationRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRepository;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceImplTest {

    @Mock private TournamentRepository tournamentRepository;
    @Mock private TournamentRegistrationRepository registrationRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TournamentMatchRepository matchRepository;
    @Mock private TournamentRankingRepository rankingRepository;
    @Mock private TournamentRankingService rankingService;
    @Mock private MessageChannel mqttOutboundChannel;

    private TournamentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TournamentServiceImpl(tournamentRepository, registrationRepository, teamRepository,
                matchRepository, rankingRepository, rankingService, new ObjectMapper());
        ReflectionTestUtils.setField(service, "mqttOutboundChannel", mqttOutboundChannel);
    }

    private Tournament tournament(String id, int max) {
        return Tournament.builder().id(id).name("T").gameTypeId("pool").status("UPCOMING")
                .maxParticipants(max).bracketMatches(new ArrayList<>()).registrations(new ArrayList<>()).build();
    }

    private TournamentRegistration reg(String pid, String name) {
        return TournamentRegistration.builder().participantId(pid).participantName(name).build();
    }

    // ── Scenario 3: generazione tabellone a eliminazione diretta (4 iscritti) ──
    @Test
    void generateBracket_4participants_buildsTwoRoundsLinkedByNextMatch() {
        Tournament t = tournament("t1", 4);
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));
        when(registrationRepository.findByTournamentId("t1"))
                .thenReturn(List.of(reg("a", "A"), reg("b", "B"), reg("c", "C"), reg("d", "D")));
        when(matchRepository.saveAll(any())).thenAnswer(inv -> {
            List<TournamentMatch> l = inv.getArgument(0);
            l.forEach(m -> { if (m.getId() == null) m.setId(UUID.randomUUID().toString()); });
            return l;
        });
        when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TournamentDto dto = service.generateBracket("t1");

        assertThat(t.getStatus()).isEqualTo("ACTIVE");
        List<TournamentMatchDto> bracket = dto.getBracket();
        assertThat(bracket).hasSize(3); // 2 semifinali + 1 finale

        List<TournamentMatchDto> round0 = bracket.stream().filter(m -> m.getRound() == 0).toList();
        List<TournamentMatchDto> round1 = bracket.stream().filter(m -> m.getRound() == 1).toList();
        assertThat(round0).hasSize(2);
        assertThat(round1).hasSize(1);

        // Tutti e 4 gli iscritti compaiono negli slot del primo round.
        assertThat(round0).flatExtracting(TournamentMatchDto::getPlayer1Name, TournamentMatchDto::getPlayer2Name)
                .containsExactlyInAnyOrder("A", "B", "C", "D");
        // La finale non ha ancora giocatori e non ha match successivo.
        assertThat(round1.get(0).getPlayer1Name()).isNull();
        assertThat(round1.get(0).getNextMatchId()).isNull();
        // Le semifinali puntano alla finale.
        String finalId = round1.get(0).getId();
        assertThat(round0).allSatisfy(m -> assertThat(m.getNextMatchId()).isEqualTo(finalId));
    }

    @Test
    void generateBracket_maxNotPowerOfTwo_badRequest() {
        Tournament t = tournament("t1", 6); // 6 non e' potenza di 2
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.generateBracket("t1"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void generateBracket_insufficientRegistrations_conflict() {
        Tournament t = tournament("t1", 4);
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));
        when(registrationRepository.findByTournamentId("t1")).thenReturn(List.of(reg("a", "A"), reg("b", "B")));

        assertThatThrownBy(() -> service.generateBracket("t1"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ── updateMatchResult: avanzamento vincitore + chiusura finale ──
    @Test
    void updateMatchResult_winnerAdvancesToNextMatch() {
        Tournament t = tournament("t1", 4);
        TournamentMatch semi = TournamentMatch.builder().id("semi0").tournament(t).round(0).matchIndex(0)
                .player1Id("a").player1Name("A").player2Id("b").player2Name("B").nextMatchId("final").build();
        TournamentMatch fin = TournamentMatch.builder().id("final").tournament(t).round(1).matchIndex(0).build();
        t.getBracketMatches().addAll(List.of(semi, fin));
        when(matchRepository.findById("semi0")).thenReturn(Optional.of(semi));
        when(matchRepository.findById("final")).thenReturn(Optional.of(fin));

        service.updateMatchResult("semi0", "a", "2-1");

        assertThat(semi.getWinnerId()).isEqualTo("a");
        assertThat(fin.getPlayer1Id()).isEqualTo("a"); // matchIndex pari -> slot player1 della finale
        assertThat(t.getStatus()).isNotEqualTo("COMPLETED");
    }

    @Test
    void updateMatchResult_finalMatch_completesTournament() {
        Tournament t = tournament("t1", 4);
        t.setStatus("ACTIVE");
        TournamentMatch fin = TournamentMatch.builder().id("final").tournament(t).round(1).matchIndex(0)
                .player1Id("a").player1Name("A").player2Id("b").player2Name("B").build(); // nextMatchId null
        t.getBracketMatches().add(fin);
        when(matchRepository.findById("final")).thenReturn(Optional.of(fin));

        service.updateMatchResult("final", "a", "3-2");

        assertThat(t.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void updateMatchResult_winnerNotInMatch_badRequest() {
        Tournament t = tournament("t1", 4);
        TournamentMatch semi = TournamentMatch.builder().id("semi0").tournament(t).round(0).matchIndex(0)
                .player1Id("a").player2Id("b").build();
        when(matchRepository.findById("semi0")).thenReturn(Optional.of(semi));
        // Nessuna squadra corrisponde: la ricerca membri restituisce vuoto per entrambi gli slot.
        lenient().when(registrationRepository.findByTournamentIdAndParticipantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMatchResult("semi0", "zzz", "1-0"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── registerToTournament: iscrizione duplicata ──
    @Test
    void registerToTournament_duplicateParticipant_rejected() {
        Tournament t = tournament("t1", 4);
        when(tournamentRepository.findById("t1")).thenReturn(Optional.of(t));
        when(registrationRepository.existsByTournamentIdAndParticipantId("t1", "a")).thenReturn(true);

        TournamentRegistrationDto dto = TournamentRegistrationDto.builder()
                .participantId("a").participantName("A").team(false).build();

        assertThatThrownBy(() -> service.registerToTournament("t1", dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── isPlayerInBracketMatch ──
    @Test
    void isPlayerInBracketMatch_playerInSlot_returnsTrue() {
        Tournament t = tournament("t1", 4);
        TournamentMatch m = TournamentMatch.builder().id("m0").tournament(t).round(0).matchIndex(0)
                .player1Id("a").player2Id("b").build();
        when(matchRepository.findById("m0")).thenReturn(Optional.of(m));
        // Slot individuali (participantId = userId): la ricerca membri-squadra non trova nulla.
        lenient().when(registrationRepository.findByTournamentIdAndParticipantId(any(), any())).thenReturn(Optional.empty());

        assertThat(service.isPlayerInBracketMatch("m0", "a")).isTrue();
        assertThat(service.isPlayerInBracketMatch("m0", "b")).isTrue();
    }
}
