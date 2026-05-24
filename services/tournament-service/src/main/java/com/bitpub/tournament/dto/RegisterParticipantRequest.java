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
    name = "RegisterParticipantRequest",
    description = "Dati dell'utente da iscrivere come partecipante a un torneo."
)
public class RegisterParticipantRequest {

    @Schema(
        description = "UUID univoco dell'utente da registrare al torneo.",
        example = "a3c2b1d0-e29b-41d4-a716-446655440002",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID userId;

    @Schema(
        description = "Username dell'utente (denormalizzato per visualizzazione rapida nella classifica).",
        example = "mario_rossi",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;
}
