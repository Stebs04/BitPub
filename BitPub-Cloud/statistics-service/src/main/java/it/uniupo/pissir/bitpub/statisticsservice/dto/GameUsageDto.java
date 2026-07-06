package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/** Uso di un tipo di gioco in un locale: metrica "Giochi piu' utilizzati in un locale". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameUsageDto {
    private String gameTypeId;
    private int matchesPlayed; // partecipazioni registrate in leaderboard per questo gioco nel locale
    private int players;       // giocatori/squadre distinti che hanno giocato questo gioco nel locale
}
