package com.bitpub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Username non può essere vuoto")
    @Size(min = 3, max = 50, message = "Username deve essere compreso tra 3 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username può contenere solo lettere, numeri e underscore")
    private String username;

    @Schema(
        description = "Password dell'utente. Verrà hashata con BCrypt prima del salvataggio.",
        example = "Secur3Pass!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 8,
        format = "password"
    )
    @NotBlank(message = "Password non può essere vuota")
    @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
    private String password;

    @Schema(
        description = "Indirizzo email valido dell'utente. Usato per notifiche e recupero account.",
        example = "mario@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "email"
    )
    @NotBlank(message = "Email non può essere vuota")
    @Email(message = "Formato email non valido")
    private String email;
}
