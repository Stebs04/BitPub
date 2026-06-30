package it.uniupo.pissir.bitpub.localeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddGameInstanceRequest {
    @NotBlank
    private String localInstanceId; // Es. "calcetto-bar-centrale-1"
    
    @NotBlank
    private String gameTypeId;
}
