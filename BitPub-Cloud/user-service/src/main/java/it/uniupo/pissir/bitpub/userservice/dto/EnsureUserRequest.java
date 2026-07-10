package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Data Transfer Object (DTO) che trasporta le informazioni necessarie
 * per garantire l'esistenza di un utente nel sistema (operazione idempotente).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnsureUserRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;
}
