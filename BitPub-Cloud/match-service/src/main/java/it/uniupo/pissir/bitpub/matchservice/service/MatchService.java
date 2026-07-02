package it.uniupo.pissir.bitpub.matchservice.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import java.util.List;
import java.util.Optional;

public interface MatchService {
    MatchDto startMatch(StartMatchRequestDto request);
    MatchDto endMatch(String matchId);
    MatchDto getMatch(String matchId);
    List<MatchDto> getActiveMatches();
    List<MatchDto> getActiveMatchesByLocale(String localeId);
    void processSensorEvent(SensorEvent event);

    /**
     * Matchmaking del PLAYER: crea una lobby in attesa sulla gameInstance scelta, o se una
     * lobby e' gia' in attesa su quella gameInstance vi aggiunge il secondo giocatore e
     * fa scattare lo stato IN_PROGRESS (evento propagato via MQTT in tempo reale).
     */
    MatchDto joinLobby(JoinLobbyRequestDto request, String playerId);

    /** Lobby in attesa di un secondo giocatore per una gameInstance, se presente. */
    Optional<MatchDto> getWaitingLobby(String gameInstanceId);

    /**
     * Processa un'azione di gioco inviata dal giocatore di turno. Se l'azione non proviene
     * dal giocatore a cui spetta il turno viene rifiutata. Aggiorna punteggi/turno secondo la
     * logica del gioco (calciobalilla, biliardo, freccette) e pubblica il nuovo stato via MQTT.
     */
    MatchDto processGameAction(String matchId, String playerId, GameActionRequestDto action);
}
