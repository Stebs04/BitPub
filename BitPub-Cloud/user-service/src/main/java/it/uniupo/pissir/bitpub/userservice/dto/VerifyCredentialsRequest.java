package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Data Transfer Object (DTO) destinato alla verifica delle credenziali 
 * di accesso fornite da un utente durante le procedure di autenticazione.
 */
@Data
public class VerifyCredentialsRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
