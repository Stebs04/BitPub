package com.bitpub.controllers;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.mqtt.CloudMqttGateway;
import com.bitpub.services.GameSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * AdminSessionController - Gestione amministrativa delle sessioni hardware.
 * * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminSessionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSessionController.class);

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private CloudMqttGateway cloudMqttGateway;

    @GetMapping("/sessions/active")
    public ResponseEntity<CollectionModel<EntityModel<GameSessionDTO>>> getActiveSessions() {
        List<GameSessionDTO> activeSessions = gameSessionService.getActiveSessions();

        List<EntityModel<GameSessionDTO>> sessionModels = activeSessions.stream().map(dto -> {
            EntityModel<GameSessionDTO> model = EntityModel.of(dto);
            model.add(linkTo(methodOn(AdminSessionController.class).forceStopSession(dto.getId())).withRel("force-stop"));
            return model;
        }).collect(Collectors.toList());

        CollectionModel<EntityModel<GameSessionDTO>> collectionModel = CollectionModel.of(sessionModels);
        collectionModel.add(linkTo(methodOn(AdminSessionController.class).getActiveSessions()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @PostMapping("/sessions/{id}/force-stop")
    public ResponseEntity<?> forceStopSession(@PathVariable Long id) {
        try {
            logger.info("Richiesta Admin: Interruzione forzata sessione {}", id);
            GameSessionDTO dto = gameSessionService.forceStopSession(id);
            return ResponseEntity.ok(EntityModel.of(dto));
            
        } catch (IllegalStateException e) {
            logger.warn("Impossibile interrompere sessione {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Errore critico durante force-stop sessione {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Errore interno durante la chiusura.",
                "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/system/edge-status")
    public ResponseEntity<?> getEdgeStatus() {
        Map<String, Instant> edgeLastSeen = cloudMqttGateway.getEdgeLastSeen();
        Instant lastSeen = edgeLastSeen.get("1"); 
        boolean isOnline = lastSeen != null && Duration.between(lastSeen, Instant.now()).getSeconds() < 30;
        
        return ResponseEntity.ok(Map.of(
                "status", isOnline ? "ONLINE" : "OFFLINE",
                "lastSeen", lastSeen != null ? lastSeen.toString() : "MAI VISTO",
                "nodeId", "1"
        ));
    }
}