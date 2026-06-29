package com.bitpub.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "LeaderboardEntry",
    description = "Voce della classifica che rappresenta le statistiche aggregate di un giocatore."
)
public class LeaderboardEntryDto {

    @Schema(
        description = "Posizione nella classifica (1 = primo posto).",
        example = "1",
        minimum = "1"
    )
    private int rank;

    @Schema(
        description = "UUID univoco del giocatore.",
        example = "a3c2b1d0-e29b-41d4-a716-446655440002"
    )
    private UUID userId;

    @Schema(
        description = "Username del giocatore.",
        example = "mario_rossi"
    )
    private String username;

    @Schema(
        description = "Numero totale di vittorie del giocatore.",
        example = "42",
        minimum = "0"
    )
    private int wins;

    @Schema(
        description = "Numero totale di sconfitte del giocatore.",
        example = "8",
        minimum = "0"
    )
    private int losses;

    @Schema(
        description = "Numero totale di partite giocate (wins + losses).",
        example = "50",
        minimum = "0"
    )
    private int totalMatches;

    @Schema(
        description = "Punteggio cumulativo del giocatore su tutte le partite.",
        example = "210",
        minimum = "0"
    )
    private int totalScore;

    @Schema(
        description = "Percentuale di vittorie calcolata come wins / totalMatches. Valore tra 0.0 e 1.0.",
        example = "0.84",
        minimum = "0.0",
        maximum = "1.0"
    )
    private double winRate;
}
