/**
 * Autore: Luca Franzon 20054744
 * Data Transfer Object utilizzato per la deserializzazione dei dati dell'utente
 * provenienti dal microservizio deputato alla loro gestione.
 */
package it.uniupo.pissir.bitpub.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String localeId;
    /*
     * Nota architetturale: qualora la responsabilità della verifica dell'hash
     * ricadesse completamente su questo strato, il campo relativo all'hash 
     * della password dovrebbe essere mappato all'interno di questa struttura.
     */
}
