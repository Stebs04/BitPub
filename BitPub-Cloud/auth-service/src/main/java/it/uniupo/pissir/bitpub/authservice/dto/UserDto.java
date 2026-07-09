package it.uniupo.pissir.bitpub.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String localeId;
    // Potremmo aggiungere passwordHash se lo strato auth lo verifica
}
