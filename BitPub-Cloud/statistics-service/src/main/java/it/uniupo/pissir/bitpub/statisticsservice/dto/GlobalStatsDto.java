package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/**
 * Vista di monitoraggio dell'intero sistema per il PLATFORM_ADMIN:
 * scala della piattaforma (locali, utenti registrati) e attivita' live (partite/tornei in corso).
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
