package com.bitpub.tournament.dto;

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
    name = "SubmitResultRequest",
    description = "Dati per la sottomissione del risultato di un incontro del torneo."
)
public class SubmitResultRequest {

    @Schema(
        description = "UUID dell'incontro (TournamentMatch) di cui si vuole registrare il risultato.",
        example = "b3c1a2d0-9dad-11d1-80b4-00c04fd430c8",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID matchId;

    @Schema(
        description = "Punteggio del giocatore A (primo partecipante dell'incontro).",
        example = "3",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int scoreA;

    @Schema(
        description = "Punteggio del giocatore B (secondo partecipante dell'incontro).",
        example = "1",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int scoreB;
}
