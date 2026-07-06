package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Azione di gioco inviata dal frontend (giocatore di turno) al match-service.
 *
 * actionType:
 *  - "SHOOT"  Calciobalilla: tiro in porta.
 *  - "BREAK"  Biliardo: spaccata iniziale (assegna Piene/Spezzate).
 *  - "SHOOT"  Biliardo: tiro normale (dopo la spaccata).
 *  - "THROW"  Freccette: singolo tiro; richiede sector e multiplier.
 *
 * sector/multiplier sono usati solo dalle Freccette:
 *  - sector: 1-20, 25 (Outer Bull) oppure 50 (Bull).
 *  - multiplier: 1, 2 o 3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionRequestDto {
    private String actionType;
    private Integer sector;
    private Integer multiplier;

    // Idempotency key set by the Edge/WebApp. If this action was already processed (e.g. a
    // buffered command replayed after the cloud came back), it is ignored and the current
    // match state is returned instead of applying the action twice.
    private String eventId;
}
