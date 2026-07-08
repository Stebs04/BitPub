package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Azione di gioco inviata dal frontend al match-service. Modello data-driven: l'azione identifica
 * solo QUALE sensore il giocatore ha premuto (sensorType, es. "GOAL", "BALL_POCKETED", "DART_HIT").
 * Il match-service inoltra l'azione al GenericSimulator, che decide l'esito con l'RNG configurato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionRequestDto {
    private String sensorType; // tipo di sensore/evento premuto dal giocatore

    // Idempotency key set by the Edge/WebApp: propagata al sensor event risultante cosi' che
    // processSensorEvent scarti eventuali repliche (QoS1) dello stesso tiro.
    private String eventId;
}
