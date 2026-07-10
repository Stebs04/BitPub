// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MatchService {
    MatchDto startMatch(StartMatchRequestDto request);
    MatchDto endMatch(String matchId);

    /**
     * Registra l'esito finale comunicato dal nodo Edge (autoritativo per i punteggi in tempo reale):
     * aggiorna i punteggi definitivi associati al nome della squadra, marca la partita come conclusa
     * e attiva le notifiche relative alle statistiche e all'avanzamento dei tornei. L'operazione è 
     * idempotente: qualora la partita sia già conclusa (COMPLETED), non viene effettuata alcuna azione.
     * Si specifica che il Cloud non gestisce più il calcolo incrementale del punteggio.
     */
    MatchDto applyFinalResult(String matchId, Map<String, Integer> scoresByTeamName);
    MatchDto getMatch(String matchId);
    List<MatchDto> getActiveMatches();
    List<MatchDto> getActiveMatchesByLocale(String localeId);
    void processSensorEvent(SensorEvent event);

    /**
     * Sistema di matchmaking per i giocatori: se esiste già una lobby in attesa per l'istanza 
     * specificata, aggiunge il secondo giocatore ed avvia la partita impostando lo stato su 
     * IN_PROGRESS (l'evento viene notificato tempestivamente via MQTT). Qualora non vi sia 
     * alcuna lobby disponibile, ne inizializza una nuova mettendola in attesa di partecipanti.
     */
    MatchDto joinLobby(JoinLobbyRequestDto request, String playerId);

    /** Restituisce i dettagli di una lobby in attesa di un secondo partecipante per una data istanza di gioco. */
    Optional<MatchDto> getWaitingLobby(String gameInstanceId);

    /**
     * Elabora l'azione di gioco inoltrata dal giocatore che possiede il turno attuale.
     * Qualora l'azione pervenga da un utente fuori dal proprio turno, essa viene respinta.
     * La funzione si occupa di inoltrare la richiesta al simulatore per l'aggiornamento 
     * del punteggio e dei turni, pubblicando successivamente le modifiche di stato.
     */
    MatchDto processGameAction(String matchId, String playerId, GameActionRequestDto action);
}
