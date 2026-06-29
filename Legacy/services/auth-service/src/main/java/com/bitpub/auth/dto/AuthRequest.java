package com.bitpub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    name = "AuthRequest",
    description = "Credenziali di accesso per il login alla piattaforma BitPub."
)
public class AuthRequest {

    @Schema(
        description = "Username univoco dell'utente registrato.",
        example = "mario_rossi",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Username non può essere vuoto")
    private String username;

    @Schema(
        description = "Password dell'utente (in chiaro nella request, trasportata via HTTPS).",
        example = "Secur3Pass!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 8,
        format = "password"
    )
    @NotBlank(message = "Password non può essere vuota")
    private String password;
}
