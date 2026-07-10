/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/**
 * Data Transfer Object contenente gli indicatori macro-sistemici utilizzati dagli amministratori di piattaforma.
 * Aggrega le informazioni generali sul volume degli utenti, dei locali operativi e sulle sessioni attive in tempo reale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalStatsDto {
    private long totalLocales;
    private long totalUsers;
    private long activeMatches;
    private long activeTournaments;
}
