package com.bitpub.tournament.dto;

import com.bitpub.tournament.model.TournamentFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.bitpub.tournament.validation.ValidTournamentRules;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidTournamentRules
@Schema(
    name = "CreateTournamentRequest",
    description = "Dati necessari per la creazione di un nuovo torneo."
)
public class CreateTournamentRequest {

    @Schema(
        description = "Nome del torneo. Deve essere univoco e descrittivo.",
        example = "BitPub Spring Cup 2024",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 3,
        maxLength = 100
    )
    @NotBlank(message = "Il nome del torneo non può essere vuoto")
    @Size(min = 3, max = 100, message = "Il nome deve essere compreso tra 3 e 100 caratteri")
    private String name;

    @Schema(
        description = "UUID del gioco a cui si riferisce il torneo.",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Il gameId non può essere vuoto")
    private String gameId;
    
    @Schema(
        description = "Formato del torneo (SINGLE_ELIMINATION o ROUND_ROBIN).",
        example = "SINGLE_ELIMINATION"
    )
    @NotNull(message = "Il formato del torneo è obbligatorio")
    private TournamentFormat format;

    @Schema(description = "Numero massimo di partecipanti (squadre o singoli).", example = "16")
    @Min(value = 2, message = "Il numero massimo di partecipanti deve essere almeno 2")
    private int maxParticipants;

    @Schema(description = "Numero di giocatori per squadra (1 = singolo giocatore).", example = "2")
    private int teamSize;

    @Schema(description = "Lista degli ID delle location dove si svolgerà.")
    private List<String> locationIds;

    @Schema(
        description = "Data e ora di inizio del torneo in formato ISO-8601 (UTC).",
        example = "2024-06-01T18:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "date-time"
    )
    @NotNull(message = "La data di inizio è obbligatoria")
    @Future(message = "La data di inizio deve essere nel futuro")
    private LocalDateTime startDate;
}
