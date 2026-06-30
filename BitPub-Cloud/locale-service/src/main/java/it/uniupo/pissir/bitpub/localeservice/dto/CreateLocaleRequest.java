package it.uniupo.pissir.bitpub.localeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLocaleRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String address;
    
    @NotBlank
    private String adminId; // O estratto dal JWT
}
