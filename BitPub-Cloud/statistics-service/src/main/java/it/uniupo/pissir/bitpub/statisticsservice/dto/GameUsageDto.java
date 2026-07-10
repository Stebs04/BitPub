/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/** Data Transfer Object per la reportistica sull'utilizzo dei giochi all'interno di un locale specifico. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameUsageDto {
    private String gameTypeId;
    private int matchesPlayed; // Totale delle partecipazioni estrapolate dalla classifica per questa specifica attività
    private int players;       // Numero di entità distinte (giocatori singoli o squadre) che hanno usufruito del gioco
}
