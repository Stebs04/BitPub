package it.uniupo.pissir.bitpub.tournamentservice.service.impl;

import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRanking;
import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRankingRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRegistrationRepository;
import it.uniupo.pissir.bitpub.tournamentservice.repository.TournamentRepository;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentRankingServiceImpl implements TournamentRankingService {

    private final TournamentRankingRepository rankingRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;

    @Override
    public List<TournamentRankingDto> getTournamentRankings(String tournamentId) {
        return rankingRepository.findByTournamentIdOrderByScoreDesc(tournamentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
        List<TournamentRanking> rankings = rankingRepository.findByTournamentIdOrderByScoreDesc(tournamentId);
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
                .score(ranking.getScore())
                .matchesPlayed(ranking.getMatchesPlayed())
                .matchesWon(ranking.getMatchesWon())
                .currentRank(ranking.getCurrentRank())
                .build();
    }
}
