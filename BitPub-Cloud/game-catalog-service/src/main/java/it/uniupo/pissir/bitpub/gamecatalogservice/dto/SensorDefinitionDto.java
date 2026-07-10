/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDefinitionDto {
    private String id;
    private String type; // Modello di ricezione o interpretazione per i segnali hardware
    private String description;
    private boolean isActuator;
    private int scoreIncrement;
    private double successProbability;
}
