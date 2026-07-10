/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddSensorRequest {
    @NotBlank
    private String type;
    
    @NotBlank
    private String description;
    
    private boolean isActuator;

    // Incremento punti di base generato all'attivazione del sensore
    private int scoreIncrement = 1;

    // Distribuzione statistica del tasso di successo (1.0 = evento garantito)
    private double successProbability = 1.0;
}
