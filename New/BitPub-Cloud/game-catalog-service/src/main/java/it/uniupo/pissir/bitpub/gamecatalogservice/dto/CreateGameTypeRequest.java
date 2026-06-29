package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGameTypeRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String description;
}
