package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Data Transfer Object (DTO) che rappresenta i dati necessari 
 * per aggiornare le informazioni principali di un profilo utente.
 */
@Data
public class UpdateUserRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
}
