package it.uniupo.pissir.bitpub.matchservice.repository;

import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByGameInstanceIdAndStatus(String gameInstanceId, String status);
    List<Match> findByStatus(String status);
    List<Match> findByLocaleIdAndStatus(String localeId, String status);
    List<Match> findByLocaleId(String localeId);
}
