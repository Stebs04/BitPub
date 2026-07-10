package it.uniupo.pissir.bitpub.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Oggetto di trasferimento dati (DTO) che espone in modo sicuro le informazioni 
 * dell'utente verso i layer superiori o i client esterni, omettendo dettagli sensibili.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String localeId;
    private Instant createdAt;
    private Instant lastLogin;
}
