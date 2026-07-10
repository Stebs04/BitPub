package it.uniupo.pissir.bitpub.localeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * DTO utilizzato per la richiesta di aggiunta di una nuova istanza di gioco ad un locale esistente.
 */
@Data
public class AddGameInstanceRequest {
    @NotBlank
    private String localInstanceId; // Identificativo locale assegnato fisicamente alla macchina, ad esempio "calcetto-bar-centrale-1"
    
    @NotBlank
    private String gameTypeId;
}
