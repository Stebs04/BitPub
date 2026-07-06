package it.uniupo.pissir.bitpub.tournamentservice.service.impl;

import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRegistrationRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRepository;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRankingService rankingService;

    @Override
    @Transactional
    public TournamentDto createTournament(TournamentDto tournamentDto) {
        Tournament tournament = Tournament.builder()
                .name(tournamentDto.getName())
                .gameTypeId(tournamentDto.getGameTypeId())
                .teamBased(tournamentDto.isTeamBased())
                .localeIds(tournamentDto.getLocaleIds())
                .startDate(tournamentDto.getStartDate())
                .endDate(tournamentDto.getEndDate())
                .status("UPCOMING")
                .build();
        tournament = tournamentRepository.save(tournament);
        return mapToDto(tournament);
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
        return dto;
    }

    @Override
    @Transactional
    public TournamentDto endTournament(String id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        tournament.setStatus("COMPLETED");
        tournament.setEndDate(Instant.now());
        return mapToDto(tournamentRepository.save(tournament));
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
                .localeId(dto.getLocaleId())
                .registeredAt(Instant.now())
                .build();
        
        registration = registrationRepository.save(registration);
        return mapRegistrationToDto(registration);
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
                .build();
        if (tournament.getRegistrations() != null) {
            dto.setRegistrations(tournament.getRegistrations().stream()
                    .map(this::mapRegistrationToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private TournamentRegistrationDto mapRegistrationToDto(TournamentRegistration reg) {
        return TournamentRegistrationDto.builder()
                .id(reg.getId())
                .tournamentId(reg.getTournament().getId())
                .participantId(reg.getParticipantId())
                .participantName(reg.getParticipantName())
                .localeId(reg.getLocaleId())
                .registeredAt(reg.getRegisteredAt())
                .build();
    }
}
