package com.bitpub.controllers;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.mqtt.CloudMqttGateway;
import com.bitpub.services.GameSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * AdminSessionController
 *
 * Refactoring Senior Note:
 * Risolta violazione SRP. Il controller delega a GameSessionService l'accesso ai dati
 * e la logica di business relativa alle sessioni (inclusa l'emissione di eventi disaccoppiati per MQTT).
 * Viene mantenuto il riferimento a CloudMqttGateway unicamente per la lettura dello stato
 * infrastrutturale in tempo reale (Edge Status), in quanto query di diagnostica di sistema.
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminSessionController {

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private CloudMqttGateway cloudMqttGateway;

    /**
     * Ritorna la lista di tutte le sessioni con status=IN_PROGRESS
     * delegando al service la lettura e costruendo un CollectionModel HATEOAS.
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<CollectionModel<EntityModel<GameSessionDTO>>> getActiveSessions() {
        List<GameSessionDTO> activeSessions = gameSessionService.getActiveSessions();

        List<EntityModel<GameSessionDTO>> sessionModels = activeSessions.stream().map(dto -> {
            EntityModel<GameSessionDTO> model = EntityModel.of(dto);
            // Aggiungiamo il link per forzare la chiusura a ogni singola risorsa
            model.add(linkTo(methodOn(AdminSessionController.class).forceStopSession(dto.getId())).withRel("force-stop"));
            return model;
        }).collect(Collectors.toList());

        CollectionModel<EntityModel<GameSessionDTO>> collectionModel = CollectionModel.of(sessionModels);
        collectionModel.add(linkTo(methodOn(AdminSessionController.class).getActiveSessions()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Interrompe forzatamente una sessione (eseguita dall'Admin). 
     * Il service si occuperà di aggiornare lo stato nel DB, scrivere l'audit log
     * e pubblicare l'evento di sblocco hardware verso MQTT in modo disaccoppiato.
     */
    @PostMapping("/sessions/{id}/force-stop")
    public ResponseEntity<?> forceStopSession(@PathVariable Long id) {
        try {
            // L'intera orchestrazione avviene nel service (Database + Audit + MQTT Event)
            GameSessionDTO dto = gameSessionService.forceStopSession(id);
            return ResponseEntity.ok(EntityModel.of(dto));
            
        } catch (IllegalStateException e) {
            // Ritorna errore se la sessione non è IN_PROGRESS o non esiste
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Si è verificato un errore inaspettato durante la chiusura forzata.");
        }
    }

    /**
     * Recupera lo stato in memoria aggiornato dal listener MQTT per scopi diagnostici.
     */
    @GetMapping("/system/edge-status")
    public ResponseEntity<?> getEdgeStatus() {
        Map<String, Instant> edgeLastSeen = cloudMqttGateway.getEdgeLastSeen();
        Instant lastSeen = edgeLastSeen.get("1"); // Ipotizziamo che "1" sia l'ID dell'Edge Node corrente
        boolean isOnline = lastSeen != null && Duration.between(lastSeen, Instant.now()).getSeconds() < 30;
        String status = isOnline ? "ONLINE" : "OFFLINE";
        String lastSeenStr = lastSeen != null ? lastSeen.toString() : "MAI VISTO";

        return ResponseEntity.ok(Map.of(
                "status", status,
                "lastSeen", lastSeenStr
        ));
    }
}