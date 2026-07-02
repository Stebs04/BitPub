package it.uniupo.pissir.bitpub.authservice.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String localeId;
    // Potremmo aggiungere passwordHash se lo strato auth lo verifica
}
