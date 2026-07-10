/**
 * Autore: Luca Franzon 20054744
 * Data Transfer Object utilizzato per incapsulare la risposta a seguito
 * di un accesso avvenuto con successo. Contiene il token di autorizzazione e
 * le informazioni essenziali del profilo utente.
 */
package it.uniupo.pissir.bitpub.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String id;
    private String username;
    private String email;
    private String role;
    private String localeId;
}
