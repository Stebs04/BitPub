package com.bitpub.controllers;

import com.bitpub.models.GameSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per il monitoraggio delle sessioni di gioco attive.
 * Le sessioni sono gestite in tempo reale dall'Edge tramite MQTT e non
 * vengono persistite nel database Cloud; questo controller espone l'endpoint
 * atteso dal client JavaFX restituendo la lista delle sessioni note al momento
 * della richiesta (implementazione base: lista vuota, estendibile con un
 * registro in-memory o Redis in futuro).
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/admin/sessions")
@CrossOrigin(origins = "*")
public class AdminSessionController {

    /**
     * Restituisce l'elenco delle sessioni di gioco attualmente attive.
     * In questa implementazione base la lista è vuota perché le sessioni
     * live risiedono nell'Edge; l'endpoint è esposto per compatibilità
     * con il client JavaFX ed è pronto per future integrazioni con un
     * registro distribuito (es. Redis, WebSocket broker).
     *
     * @return {@link ResponseEntity} con la lista (eventualmente vuota) delle {@link GameSession}.
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GameSession>> getActiveSessions() {
        // TODO: integrare con un registro in-memory o Redis quando le sessioni
        // verranno centralizzate lato Cloud.
        return ResponseEntity.ok(List.of());
    }

    /**
     * Forza la chiusura di una sessione identificata dal suo ID.
     * Questo endpoint è il target dell'operazione "Sblocco Forzato" nel pannello Admin.
     *
     * @param sessionId Identificativo univoco della sessione da terminare.
     * @return {@link ResponseEntity} 200 OK al completamento.
     */
    @PostMapping("/stop/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> stopSession(@PathVariable String sessionId) {
        // TODO: inviare un comando MQTT all'Edge per terminare la sessione specificata.
        System.out.println("[ADMIN] Richiesta di stop forzato per la sessione: " + sessionId);
        return ResponseEntity.ok().build();
    }
}
