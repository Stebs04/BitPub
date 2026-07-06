package it.uniupo.pissir.bitpub.matchservice.repository;

import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    // findFirst tollera duplicati (es. due lobby WAITING_FOR_PLAYERS create in race) senza NonUniqueResultException.
    // ponytail: prende il piu' recente per startTime; se le lobby duplicate diventano un problema reale, dedup a monte in joinLobby.
    Optional<Match> findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(String gameInstanceId, String status);
    List<Match> findByStatus(String status);
    List<Match> findByLocaleIdAndStatus(String localeId, String status);
    List<Match> findByLocaleId(String localeId);
}
