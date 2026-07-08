package it.uniupo.pissir.bitpub.tournamentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.common.mqtt.TournamentResultCommand;
import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentMatch;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentMatchDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentMatchRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRankingRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRegistrationRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRepository;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentMatchRepository matchRepository;
    private final TournamentRankingRepository rankingRepository;
    private final TournamentRankingService rankingService;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier("mqttOutboundChannel")
    private MessageChannel mqttOutboundChannel;

    /**
     * Ingerisce l'esito manuale di uno scontro inviato dall'Edge su MQTT (QoS1). Identita' e ruolo
     * viaggiano nel wrapper (MQTT non ha header): solo il LOCALE_ADMIN puo' registrare un esito.
     * Cattura le eccezioni per non far crashare il subscriber ne' nack-are il messaggio QoS1
     * (un replay ripeterebbe lo stesso esito deterministico).
     * ponytail: il vecchio path REST verificava anche la proprieta' del locale (assertOwns). Il
     * wrapper non porta il localeId, quindi qui si controlla solo il ruolo; per ripristinare il
     * controllo di proprieta' aggiungere il localeId a MqttCommandWrapper e riverificarlo.
     */
    @ServiceActivator(inputChannel = "mqttResultInboundChannel")
    public void onTournamentResult(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            if (!"LOCALE_ADMIN".equals(wrapper.actorRole())) {
                log.warn("Rifiutato esito torneo via MQTT: ruolo {} non autorizzato", wrapper.actorRole());
                return;
            }
            TournamentResultCommand cmd = objectMapper.readValue(wrapper.payload(), TournamentResultCommand.class);
            updateMatchResult(cmd.matchId(), cmd.winnerId(), cmd.stats());
            log.info("Esito torneo processato via MQTT: match={}, winner={}", cmd.matchId(), cmd.winnerId());
        } catch (BitpubException e) {
            // Rifiuto legittimo (winnerId non valido, match/torneo inesistente). ResourceNotFoundException
            // estende BitpubException, quindi rientra qui. Non far crashare il subscriber.
            log.info("Esito torneo via MQTT rifiutato: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Impossibile processare esito torneo inbound: {}", message.getPayload(), e);
        }
    }

    /** Pubblica il TournamentDto aggiornato sul topic live cosi' il WebApp aggiorna il tabellone in tempo reale. */
    private void publishState(TournamentDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            mqttOutboundChannel.send(MessageBuilder.withPayload(json)
                    .setHeader(MqttHeaders.TOPIC, MqttTopics.getTournamentStateTopic(dto.getId()))
                    .build());
        } catch (Exception e) {
            // Best-effort: un errore di publish non deve far fallire la transazione del torneo.
            log.error("Publish stato torneo {} fallito", dto.getId(), e);
        }
    }

    @Override
    @Transactional
    public TournamentDto createTournament(TournamentDto tournamentDto) {
        Tournament tournament = Tournament.builder()
                .name(tournamentDto.getName())
                .gameTypeId(tournamentDto.getGameTypeId())
                .teamBased(tournamentDto.isTeamBased())
                // Lista mutabile: Hibernate deve poter fare clear/merge sull'@ElementCollection.
                .localeIds(tournamentDto.getLocaleIds() == null ? new ArrayList<>() : new ArrayList<>(tournamentDto.getLocaleIds()))
                .startDate(tournamentDto.getStartDate())
                .endDate(tournamentDto.getEndDate())
                .status("UPCOMING")
                .maxParticipants(tournamentDto.getMaxParticipants())
                .build();
        tournament = tournamentRepository.save(tournament);
        return mapToDto(tournament);
    }

    /**
     * Modifica consentita solo finche' nessun giocatore/squadra e' iscritto: cambiare gioco,
     * locali o modalita' dopo le iscrizioni invaliderebbe i partecipanti gia' registrati.
     */
    @Override
    @Transactional
    public TournamentDto updateTournament(String id, TournamentDto dto) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        if (!registrationRepository.findByTournamentId(id).isEmpty()) {
            throw new BitpubException("Impossibile modificare un torneo con iscritti", HttpStatus.CONFLICT);
        }
        tournament.setName(dto.getName());
        tournament.setGameTypeId(dto.getGameTypeId());
        tournament.setTeamBased(dto.isTeamBased());
        // Lista mutabile: passare List.of(...) qui fa fallire il merge Hibernate con
        // UnsupportedOperationException su ImmutableCollections.clear.
        tournament.setLocaleIds(dto.getLocaleIds() == null ? new ArrayList<>() : new ArrayList<>(dto.getLocaleIds()));
        tournament.setMaxParticipants(dto.getMaxParticipants());
        return mapToDto(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public void deleteTournament(String id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        // Rimuove prima le classifiche (tabella separata, non in cascade), poi il torneo
        // che cascata su registrazioni e locali coinvolti.
        rankingRepository.deleteAll(rankingRepository.findByTournamentIdOrderByScoreDesc(id));
        tournamentRepository.delete(tournament);
    }

    @Override
    public TournamentDto getTournament(String id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        return mapToDto(tournament);
    }

    @Override
    public List<TournamentDto> getAllTournaments() {
        return tournamentRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<TournamentDto> getActiveTournaments() {
        return tournamentRepository.findByStatus("ACTIVE").stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TournamentDto startTournament(String id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        tournament.setStatus("ACTIVE");
        tournament.setStartDate(Instant.now());
        TournamentDto dto = mapToDto(tournamentRepository.save(tournament));
        // All'avvio crea una riga di classifica per ogni iscritto, cosi' la classifica esiste
        // gia' a torneo iniziato (poi i punteggi si sincronizzano dai match via statistics-service).
        rankingService.initializeRankingsForTournament(id);
        publishState(dto);
        return dto;
    }

    @Override
    @Transactional
    public TournamentDto endTournament(String id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        tournament.setStatus("COMPLETED");
        tournament.setEndDate(Instant.now());
        TournamentDto dto = mapToDto(tournamentRepository.save(tournament));
        publishState(dto);
        return dto;
    }

    /**
     * Player's own tournament registrations — used by the dashboard "my tournaments" view.
     * No repository finder exists by participantId, so filter in-memory.
     */
    @Transactional(readOnly = true)
    public List<TournamentRegistrationDto> getRegistrationsByParticipant(String participantId) {
        return registrationRepository.findAll().stream()
                .filter(r -> r.getParticipantId().equals(participantId))
                .map(this::mapRegistrationToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TournamentRegistrationDto registerToTournament(String tournamentId, TournamentRegistrationDto dto) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));
        
        if (registrationRepository.existsByTournamentIdAndParticipantId(tournamentId, dto.getParticipantId())) {
            throw new IllegalArgumentException("Participant already registered to this tournament");
        }

        TournamentRegistration registration = TournamentRegistration.builder()
                .tournament(tournament)
                .participantId(dto.getParticipantId())
                .participantName(dto.getParticipantName())
                .team(dto.isTeam())
                .members(dto.getMembers())
                .localeId(dto.getLocaleId())
                .registeredAt(Instant.now())
                .build();
        
        registration = registrationRepository.save(registration);

        // Avvio automatico: al raggiungimento di maxParticipants il tabellone si genera da solo
        // (l'admin non avvia piu' manualmente). Guardia su bracket vuoto per non ri-generare.
        Integer max = tournament.getMaxParticipants();
        boolean bracketEmpty = tournament.getBracketMatches() == null || tournament.getBracketMatches().isEmpty();
        if (max != null && bracketEmpty
                && registrationRepository.findByTournamentId(tournamentId).size() >= max) {
            generateBracket(tournamentId);
        }

        return mapRegistrationToDto(registration);
    }

    /**
     * Genera il tabellone a eliminazione diretta quando gli iscritti raggiungono maxParticipants.
     * maxParticipants deve essere potenza di 2. Randomizza gli iscritti, crea i match di ogni round
     * e li collega tramite nextMatchId. Il torneo passa ad ACTIVE.
     */
    @Override
    @Transactional
    public TournamentDto generateBracket(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        Integer max = tournament.getMaxParticipants();
        if (max == null || max < 2) {
            throw new BitpubException("maxParticipants non impostato o minore di 2", HttpStatus.BAD_REQUEST);
        }
        if ((max & (max - 1)) != 0) {
            throw new BitpubException("maxParticipants deve essere una potenza di 2 (4, 8, 16...)", HttpStatus.BAD_REQUEST);
        }
        if (tournament.getBracketMatches() != null && !tournament.getBracketMatches().isEmpty()) {
            throw new BitpubException("Tabellone gia' generato per questo torneo", HttpStatus.CONFLICT);
        }

        List<TournamentRegistration> regs = new ArrayList<>(registrationRepository.findByTournamentId(tournamentId));
        if (regs.size() < max) {
            throw new BitpubException("Iscritti insufficienti: " + regs.size() + "/" + max, HttpStatus.CONFLICT);
        }
        Collections.shuffle(regs);
        regs = regs.subList(0, max);

        int rounds = Integer.numberOfTrailingZeros(max); // log2(max)
        List<List<TournamentMatch>> byRound = new ArrayList<>();
        for (int r = 0; r < rounds; r++) {
            int count = max >> (r + 1);
            List<TournamentMatch> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TournamentMatch m = TournamentMatch.builder()
                        .tournament(tournament).round(r).matchIndex(i).build();
                if (r == 0) {
                    TournamentRegistration p1 = regs.get(2 * i);
                    TournamentRegistration p2 = regs.get(2 * i + 1);
                    m.setPlayer1Id(p1.getParticipantId());
                    m.setPlayer1Name(p1.getParticipantName());
                    m.setPlayer2Id(p2.getParticipantId());
                    m.setPlayer2Name(p2.getParticipantName());
                }
                list.add(m);
            }
            byRound.add(list);
        }

        List<TournamentMatch> all = byRound.stream().flatMap(List::stream).collect(Collectors.toList());
        matchRepository.saveAll(all); // primo save: assegna gli id (UUID)
        for (int r = 0; r < rounds - 1; r++) {
            List<TournamentMatch> next = byRound.get(r + 1);
            for (TournamentMatch m : byRound.get(r)) {
                m.setNextMatchId(next.get(m.getMatchIndex() / 2).getId());
            }
        }
        matchRepository.saveAll(all); // secondo save: persiste i collegamenti nextMatchId

        tournament.setStatus("ACTIVE");
        tournament.setStartDate(Instant.now());
        // Muta la collection gestita in-place: sostituirla con una nuova lista rompe
        // orphan-removal (HibernateException "no longer referenced by the owning entity").
        if (tournament.getBracketMatches() == null) {
            tournament.setBracketMatches(new ArrayList<>());
        }
        tournament.getBracketMatches().clear();
        tournament.getBracketMatches().addAll(all);
        tournamentRepository.save(tournament);
        // Classifica inizializzata all'attivazione (prima lo faceva startTournament, ora rimosso).
        rankingService.initializeRankingsForTournament(tournamentId);
        TournamentDto dto = mapToDto(tournament);
        publishState(dto);
        return dto;
    }

    /**
     * Registra il vincitore (e le statistiche testuali) di uno scontro e lo fa avanzare nel match
     * successivo. Se e' la finale, il torneo passa a COMPLETED. Il tabellone resta consultabile.
     */
    @Override
    @Transactional
    public TournamentDto updateMatchResult(String matchId, String winnerId, String stats) {
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        boolean p1won = winnerId != null && winnerId.equals(match.getPlayer1Id());
        boolean p2won = winnerId != null && winnerId.equals(match.getPlayer2Id());
        if (!p1won && !p2won) {
            throw new BitpubException("winnerId non e' un giocatore di questo scontro", HttpStatus.BAD_REQUEST);
        }

        match.setWinnerId(winnerId);
        match.setWinnerName(p1won ? match.getPlayer1Name() : match.getPlayer2Name());
        match.setScore(stats);
        matchRepository.save(match);

        if (match.getNextMatchId() != null) {
            TournamentMatch next = matchRepository.findById(match.getNextMatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match not found: " + match.getNextMatchId()));
            if (match.getMatchIndex() % 2 == 0) {
                next.setPlayer1Id(match.getWinnerId());
                next.setPlayer1Name(match.getWinnerName());
            } else {
                next.setPlayer2Id(match.getWinnerId());
                next.setPlayer2Name(match.getWinnerName());
            }
            matchRepository.save(next);
        } else {
            Tournament tournament = match.getTournament();
            tournament.setStatus("COMPLETED");
            tournament.setEndDate(Instant.now());
            tournamentRepository.save(tournament);
        }
        TournamentDto dto = mapToDto(match.getTournament());
        publishState(dto);
        return dto;
    }

    /**
     * Il player e' uno dei due giocatori abbinati nello scontro del tabellone.
     * Usato da match-service per impedire a chi non e' abbinato di connettersi alla partita.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPlayerInBracketMatch(String matchId, String playerId) {
        return matchRepository.findById(matchId)
                .map(m -> playerId != null
                        && (playerId.equals(m.getPlayer1Id()) || playerId.equals(m.getPlayer2Id())))
                .orElse(false);
    }

    private TournamentDto mapToDto(Tournament tournament) {
        TournamentDto dto = TournamentDto.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .gameTypeId(tournament.getGameTypeId())
                .teamBased(tournament.isTeamBased())
                .localeIds(tournament.getLocaleIds())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .status(tournament.getStatus())
                .maxParticipants(tournament.getMaxParticipants())
                .build();
        if (tournament.getRegistrations() != null) {
            dto.setRegistrations(tournament.getRegistrations().stream()
                    .map(this::mapRegistrationToDto)
                    .collect(Collectors.toList()));
        }
        if (tournament.getBracketMatches() != null) {
            dto.setBracket(tournament.getBracketMatches().stream()
                    .sorted(Comparator.comparingInt(TournamentMatch::getRound)
                            .thenComparingInt(TournamentMatch::getMatchIndex))
                    .map(this::mapMatchToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private TournamentMatchDto mapMatchToDto(TournamentMatch m) {
        return TournamentMatchDto.builder()
                .id(m.getId())
                .round(m.getRound())
                .matchIndex(m.getMatchIndex())
                .player1Id(m.getPlayer1Id())
                .player1Name(m.getPlayer1Name())
                .player2Id(m.getPlayer2Id())
                .player2Name(m.getPlayer2Name())
                .winnerId(m.getWinnerId())
                .winnerName(m.getWinnerName())
                .score(m.getScore())
                .nextMatchId(m.getNextMatchId())
                .build();
    }

    private TournamentRegistrationDto mapRegistrationToDto(TournamentRegistration reg) {
        return TournamentRegistrationDto.builder()
                .id(reg.getId())
                .tournamentId(reg.getTournament().getId())
                .participantId(reg.getParticipantId())
                .participantName(reg.getParticipantName())
                .team(reg.isTeam())
                .members(reg.getMembers())
                .localeId(reg.getLocaleId())
                .registeredAt(reg.getRegisteredAt())
                .build();
    }
}
