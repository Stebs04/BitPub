package com.bitpub.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "RegisterTeamRequest",
    description = "Dati del team (o singolo giocatore) da iscrivere a un torneo."
)
public class RegisterTeamRequest {

    @Schema(
        description = "Nome della squadra o del giocatore se singolo.",
        example = "I Leoni",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Il nome della squadra non può essere vuoto")
    private String name;

    @Schema(
        description = "Lista dei giocatori (utenti) che compongono la squadra.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "La squadra deve avere almeno un giocatore")
    @Valid
    private List<PlayerDto> players;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerDto {
        @Schema(description = "UUID univoco dell'utente.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "L'ID dell'utente è obbligatorio")
        private UUID userId;

        @Schema(description = "Username dell'utente.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Lo username dell'utente è obbligatorio")
        private String username;
    }
}
