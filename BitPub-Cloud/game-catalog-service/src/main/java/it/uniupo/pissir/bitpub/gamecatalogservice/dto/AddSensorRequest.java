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

    // Punti guadagnati quando il sensore si attiva. Default 1.
    private int scoreIncrement = 1;

    // Probabilità RNG (0.0-1.0) di successo dell'azione. Default 1.0 (sempre a segno).
    private double successProbability = 1.0;
}
