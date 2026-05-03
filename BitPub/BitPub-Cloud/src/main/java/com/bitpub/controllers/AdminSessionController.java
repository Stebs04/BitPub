package com.bitpub.controllers;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.mqtt.CloudMqttGateway;
import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.repository.GameSessionEntity;
import com.bitpub.repository.GameSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminSessionController {

    @Autowired
    private GameSessionRepository gameSessionRepository;

   
     @Autowired
     private CloudMqttGateway cloudMqttGateway;

    
    @Autowired
    private AuditLogRepository auditLogRepository;
    

    /**
     * Ritorna la lista di tutte le GameSessionEntity con status=IN_PROGRESS
     * restituendo un CollectionModel HATEOAS.
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<CollectionModel<EntityModel<GameSessionDTO>>> getActiveSessions() {
        List<GameSessionEntity> activeSessions = gameSessionRepository.findAllByStatus("IN_PROGRESS");

        List<EntityModel<GameSessionDTO>> sessionModels = activeSessions.stream().map(session -> {
            GameSessionDTO dto = new GameSessionDTO(session);
            EntityModel<GameSessionDTO> model = EntityModel.of(dto);
            // Aggiungiamo il link per forzare la chiusura a ogni singola risorsa
            model.add(linkTo(methodOn(AdminSessionController.class).forceStopSession(session.getId())).withRel("force-stop"));
            return model;
        }).collect(Collectors.toList());

        CollectionModel<EntityModel<GameSessionDTO>> collectionModel = CollectionModel.of(sessionModels);
        collectionModel.add(linkTo(methodOn(AdminSessionController.class).getActiveSessions()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Aggiorna lo stato di una sessione bloccata, pubblica il comando MQTT
     * per sbloccare l'hardware e salva l'operazione nell'audit_log.
     */
    @PostMapping("/sessions/{id}/force-stop")
    public ResponseEntity<?> forceStopSession(@PathVariable Long id) {
        GameSessionEntity session = gameSessionRepository.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            return ResponseEntity.badRequest().body("La sessione non è in corso e non può essere forzata.");
        }

        // Aggiornamento stato DB
        session.setStatus("FORCE_STOPPED");
        session.setFinishedAt(LocalDateTime.now());
        gameSessionRepository.save(session);

        
        cloudMqttGateway.publishForceStop(session.getTableId());

        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("WARN");
        log.setSource("Cloud-Admin");
        log.setAction("SESSION_FORCE_STOPPED");
        log.setMessage("Sessione " + id + " interrotta forzatamente da Admin");
        auditLogRepository.save(log);

        GameSessionDTO dto = new GameSessionDTO(session);
        return ResponseEntity.ok(EntityModel.of(dto));
    }

    /**
     * Recupera lo stato in memoria aggiornato dal listener MQTT.
     */
    @GetMapping("/system/edge-status")
    public ResponseEntity<?> getEdgeStatus() {
      
        Map<String, Instant> edgeLastSeen = cloudMqttGateway.getEdgeLastSeen();
        Instant lastSeen = edgeLastSeen.get("1");
        boolean isOnline = lastSeen != null && Duration.between(lastSeen, Instant.now()).getSeconds() < 30;
        String status = isOnline ? "ONLINE" : "OFFLINE";
        String lastSeenStr = lastSeen != null ? lastSeen.toString() : "MAI VISTO";

        return ResponseEntity.ok(Map.of(
                "status", status,
                "lastSeen", lastSeenStr
        ));
    }
}
