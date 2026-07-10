/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGameTypeRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String description;

    // Obiettivo di punteggio predefinito per la vittoria
    private int winScoreTarget = 10;
}
