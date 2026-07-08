package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGameTypeRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String description;

    // Punti necessari per vincere. Default 10 se non specificato dal client.
    private int winScoreTarget = 10;
}
