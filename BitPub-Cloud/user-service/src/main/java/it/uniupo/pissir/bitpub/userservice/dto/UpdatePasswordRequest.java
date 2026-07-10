package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Data Transfer Object (DTO) utilizzato per gestire le richieste di aggiornamento 
 * della password di un utente esistente.
 */
@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "Password is required")
    private String newPassword;
}
