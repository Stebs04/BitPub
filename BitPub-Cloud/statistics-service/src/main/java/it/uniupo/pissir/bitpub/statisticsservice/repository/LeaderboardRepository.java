/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, String> {

    /** Estrae l'intera classifica per un gioco specifico, ordinando per vittorie e poi per punteggio totale. */
    List<Leaderboard> findByGameTypeIdOrderByWinsDescTotalPointsDesc(String gameTypeId);

    /** Ricerca lo specifico posizionamento in classifica di un giocatore all'interno di un dato gioco. */
    Optional<Leaderboard> findByPlayerNameIgnoreCaseAndGameTypeId(String playerName, String gameTypeId);

    /** Recupera in un'unica operazione tutte le presenze in classifica di un singolo giocatore su ogni disciplina. */
    List<Leaderboard> findByPlayerNameIgnoreCase(String playerName);

    /** Ritorna la top 10 globale calcolata trasversalmente su tutti i giochi disponibili. */
    List<Leaderboard> findTop10ByOrderByWinsDescTotalPointsDesc();

    /** Filtra e ordina la classifica per un gioco circoscrivendo la ricerca a un singolo locale (utile per gli admin). */
    List<Leaderboard> findByGameTypeIdAndLocaleIdOrderByWinsDescTotalPointsDesc(String gameTypeId, String localeId);

    /** Ottiene tutte le entry di classifica maturate in un locale, funzionale al calcolo dei giochi più popolari. */
    List<Leaderboard> findByLocaleId(String localeId);

    /** Identifica dinamicamente le tipologie di gioco in cui il giocatore vanta almeno una partecipazione. */
    @Query("SELECT DISTINCT l.gameTypeId FROM Leaderboard l WHERE l.playerName = :playerName")
    List<String> findDistinctGameTypeIdsByPlayerName(String playerName);
}
