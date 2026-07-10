// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.repository;

import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    // Utilizziamo findFirst per prevenire NonUniqueResultException qualora si verificassero race condition 
    // che portano alla creazione di lobby duplicate (es. due lobby WAITING_FOR_PLAYERS).
    // Viene selezionata la lobby più recente in base al timestamp di inizio. Qualora l'esistenza di
    // lobby duplicate diventi problematica, sarà opportuno implementare meccanismi di deduplicazione
    // a monte, all'interno della logica di joinLobby.
    Optional<Match> findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(String gameInstanceId, String status);
    List<Match> findByStatus(String status);
    List<Match> findByLocaleIdAndStatus(String localeId, String status);
    List<Match> findByLocaleId(String localeId);
}
