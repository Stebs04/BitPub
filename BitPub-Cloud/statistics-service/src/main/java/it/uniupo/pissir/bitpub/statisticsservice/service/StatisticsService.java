/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.service;

import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GameUsageDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;

import java.util.List;

public interface StatisticsService {
    List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType);
    AggregateStatisticDto updateStatistic(StatisticUpdateRequest request);

    /** Registra permanentemente il risultato di un match, aggiornando le relative posizioni in classifica. */
    void recordMatchResult(MatchResultEvent event);

    /** Operazione di backfill: azzera l'attuale classifica e la ricalcola da zero iterando sullo storico completo. */
    int rebuildLeaderboard(List<MatchResultEvent> events);

    /** Fornisce la classifica ordinata per un singolo gioco (prima per vittorie, poi per punti). */
    List<LeaderboardEntryDto> getLeaderboard(String gameTypeId);

    /** Recupera l'elenco dei giochi in cui l'utente risulta classificato. */
    List<String> getGameTypeIdsForPlayer(String playerName);

    /** Interroga il database per estrarre il profilo statistico completo del giocatore in ogni disciplina. */
    List<LeaderboardEntryDto> getMyLeaderboardEntries(String playerName);

    /** Ottiene la classifica di un gioco, circoscritta però ai soli match disputati in uno specifico locale. */
    List<LeaderboardEntryDto> getLeaderboardByLocale(String gameTypeId, String localeId);

    /** Genera la reportistica di utilizzo dei giochi all'interno del locale, con ordinamento decrescente. */
    List<GameUsageDto> getMostUsedGamesByLocale(String localeId);

    /** Identifica a quale locale è associato un amministratore, utile per i filtri di sicurezza. */
    String resolveAdminLocaleId(String adminId);

    /** Compila un quadro riassuntivo globale sullo stato del sistema a uso e consumo dei PLATFORM_ADMIN. */
    GlobalStatsDto getGlobalOverview();
}

