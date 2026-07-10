/**
 * Autore: Luca Franzon 20054744
 * Data Transfer Object che rappresenta il payload della richiesta di autenticazione.
 * Specifica i vincoli formali necessari per assicurare l'integrità dei dati inviati dal client.
 */
package it.uniupo.pissir.bitpub.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
}
