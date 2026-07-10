package it.uniupo.pissir.bitpub.localeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Data Transfer Object per la rappresentazione delle informazioni di base di un'istanza di gioco.
 * Utilizzato per esporre i dati tramite le API senza rivelare direttamente le entita' del dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameInstanceDto {
    private String id;
    private String localInstanceId;
    private String gameTypeId;
    private String localeId;
    private Instant installedAt;
    private boolean active;
}
