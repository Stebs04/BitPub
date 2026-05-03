package com.bitpub.controllers;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.mqtt.CloudMqttGateway;
import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.repository.GameSessionEntity;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.repository.UtenteRepository;
import com.bitpub.models.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/sessions/foosball")
public class FoosballSessionController {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private CloudMqttGateway cloudMqttGateway;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Integer> payload) {
        Integer tableId = payload.get("table_id");
        if (tableId == null) {
            return ResponseEntity.badRequest().body("Il campo 'table_id' è obbligatorio nel JSON.");
        }

        // Estrazione ID Utente dal Security Context (basato su JWT stateless)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<Utente> userOpt = utenteRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
        }
        Long userId = userOpt.get().getId();

        // Verifica che l'utente non abbia già una partita IN_PROGRESS
        Optional<GameSessionEntity> activeSession = gameSessionRepository.findByUserIdAndStatus(userId, "IN_PROGRESS");
        if (activeSession.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Hai già una partita in corso (ID: " + activeSession.get().getId() + "). Termina quella prima di iniziarne una nuova.");
        }

        // --- QUI ANDREBBE IL CONTROLLO DEL CREDITO ---
        // Ipotizzando di avere un UserRepository: user.getCredit().compareTo(fixedCost) >= 0
        BigDecimal fixedCost = new BigDecimal("1.00");

        // Crea e salva la sessione in PostgreSQL
        GameSessionEntity newSession = GameSessionEntity.builder()
                .gameType("FOOSBALL")
                .tableId(tableId)
                .userId(userId)
                .status("IN_PROGRESS")
                .scoreBlue(0)
                .scoreRed(0)
                .costDeducted(fixedCost)
                .build();
        
        newSession = gameSessionRepository.save(newSession);

        
        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("INFO");
        log.setSource("Cloud-Foosball");
        log.setAction("SESSION_STARTED");
        log.setMessage("Partita avviata su tavolo " + tableId + " da utente ID " + userId);
        auditLogRepository.save(log);

      
        cloudMqttGateway.publishUnlockBalls(tableId);

        // Costruzione risposta HATEOAS
        GameSessionDTO dto = new GameSessionDTO(newSession);
        EntityModel<GameSessionDTO> entityModel = EntityModel.of(dto);
        
        // Link "self"
        entityModel.add(linkTo(methodOn(FoosballSessionController.class).getCurrentSession()).withSelfRel());
        
        // Link "force-stop" per gli admin
        entityModel.add(Link.of("/api/v1/admin/sessions/" + newSession.getId() + "/force-stop").withRel("force-stop"));
        
        // Link "dashboard" per navigazione base
        entityModel.add(Link.of("/api/v1/dashboard").withRel("dashboard"));

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSession() {
        // Estrazione ID Utente
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<Utente> userOpt = utenteRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
        }
        Long userId = userOpt.get().getId();

        // Cerca la partita IN_PROGRESS di questo utente
        Optional<GameSessionEntity> activeSession = gameSessionRepository.findByUserIdAndStatus(userId, "IN_PROGRESS");
        
        if (activeSession.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessuna partita in corso per l'utente.");
        }

        // Mapping e HATEOAS
        GameSessionDTO dto = new GameSessionDTO(activeSession.get());
        EntityModel<GameSessionDTO> entityModel = EntityModel.of(dto);
        entityModel.add(linkTo(methodOn(FoosballSessionController.class).getCurrentSession()).withSelfRel());
        
        return ResponseEntity.ok(entityModel);
    }
}
