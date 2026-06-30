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
}
