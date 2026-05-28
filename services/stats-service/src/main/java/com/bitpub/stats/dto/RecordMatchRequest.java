package com.bitpub.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "RecordMatchRequest",
    description = """
        Richiesta di registrazione del risultato di una partita completata.
        
        Questo DTO viene tipicamente inviato dall'Edge-Sync Service o dal Game Service
        al termine di ogni match. La stessa `matchSessionId` è idempotente.
        """
)
public class RecordMatchRequest {

    @Schema(
        description = "UUID univoco della sessione di partita. Usato per garantire l'idempotenza.",
        example = "d4e5f6a7-b8c9-41d4-a716-446655440002",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "matchSessionId obbligatorio")
    private UUID matchSessionId;

    @Schema(
        description = "UUID del gioco a cui si riferisce la partita.",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "gameId obbligatorio")
    private UUID gameId;

    @Schema(
        description = "UUID dell'utente vincitore.",
        example = "a3c2b1d0-e29b-41d4-a716-446655440002",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "winnerUserId obbligatorio")
    private UUID winnerUserId;

    @Schema(
        description = "Username del vincitore (denormalizzato per evitare join sui report).",
        example = "mario_rossi",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "winnerUsername obbligatorio")
    private String winnerUsername;

    @Schema(
        description = "UUID dell'utente perdente.",
        example = "b4d3c2e1-f30c-52e5-b827-557766551113",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "loserUserId obbligatorio")
    private UUID loserUserId;

    @Schema(
        description = "Username del perdente (denormalizzato).",
        example = "luigi_verdi",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "loserUsername obbligatorio")
    private String loserUsername;

    @Schema(
        description = "Punteggio finale del vincitore.",
        example = "5",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 0, message = "Il punteggio non può essere negativo")
    private int winnerScore;

    @Schema(
        description = "Punteggio finale del perdente.",
        example = "2",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 0, message = "Il punteggio non può essere negativo")
    private int loserScore;
}
