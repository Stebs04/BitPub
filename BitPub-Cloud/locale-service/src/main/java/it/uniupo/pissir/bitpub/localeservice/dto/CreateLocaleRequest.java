package it.uniupo.pissir.bitpub.localeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * DTO utilizzato per raccogliere i dati necessari alla registrazione di un nuovo locale nel sistema.
 */
@Data
public class CreateLocaleRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String address;
    
    @NotBlank
    private String adminId; // Identificativo dell'amministratore del locale. Puo' essere estratto dal JWT del chiamante o fornito direttamente.
}
