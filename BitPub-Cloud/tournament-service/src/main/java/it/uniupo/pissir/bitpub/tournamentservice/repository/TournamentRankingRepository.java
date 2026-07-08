package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRankingRepository extends JpaRepository<TournamentRanking, String> {
    List<TournamentRanking> findByTournamentIdOrderByScoreDesc(String tournamentId);
    List<TournamentRanking> findByTournamentIdOrderByMatchesWonDesc(String tournamentId);
    Optional<TournamentRanking> findByTournamentIdAndParticipantId(String tournamentId, String participantId);
}
