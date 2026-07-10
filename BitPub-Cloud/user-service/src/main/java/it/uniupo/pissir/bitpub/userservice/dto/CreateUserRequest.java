package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Data Transfer Object (DTO) utilizzato per incapsulare i dati di richiesta 
 * durante la creazione di un nuovo utente.
 */
@Data
public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Role is required")
    private String role;
}
