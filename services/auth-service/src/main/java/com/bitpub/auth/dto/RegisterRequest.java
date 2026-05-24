package com.bitpub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    name = "RegisterRequest",
    description = "Dati richiesti per la registrazione di un nuovo utente sulla piattaforma BitPub."
)
public class RegisterRequest {

    @Schema(
        description = "Username univoco dell'utente. Deve essere composto da lettere, numeri e underscore.",
        example = "mario_rossi",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 3,
        maxLength = 50,
        pattern = "^[a-zA-Z0-9_]+$"
    )
    private String username;

    @Schema(
        description = "Password dell'utente. Verrà hashata con BCrypt prima del salvataggio.",
        example = "Secur3Pass!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 8,
        format = "password"
    )
    private String password;

    @Schema(
        description = "Indirizzo email valido dell'utente. Usato per notifiche e recupero account.",
        example = "mario@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "email"
    )
    private String email;
}
