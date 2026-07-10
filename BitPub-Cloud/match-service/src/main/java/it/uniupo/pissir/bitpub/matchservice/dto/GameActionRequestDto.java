// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO rappresentante un'azione di gioco inoltrata dal frontend al servizio di gestione partite.
 * Modello basato sui dati: la richiesta specifica unicamente l'identificativo del sensore 
 * attivato dal giocatore (es. "GOAL", "BALL_POCKETED", "DART_HIT").
 * Il servizio provvede successivamente ad inoltrare l'evento al simulatore, che valuterà 
 * l'esito finale dell'azione applicando logiche di generazione causale (RNG).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameActionRequestDto {
    private String sensorType; // Tipologia del sensore o evento attivato dall'utente

    // Chiave di idempotenza impostata dal nodo Edge o dalla WebApp: viene propagata all'evento 
    // generato per consentire al metodo di elaborazione di scartare eventuali duplicati dello stesso tiro.
    private String eventId;
}
