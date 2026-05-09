package com.bitpub.controllers;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.services.GameSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * FoosballSessionController
 *
 * Refactoring Senior Note:
 * Risolta la violazione del Single Responsibility Principle (SRP).
 * Il controller non interagisce più direttamente con le repository JPA o con il gateway MQTT.
 * Tutta la logica di business, la persistenza e la pubblicazione degli eventi di dominio
 * sono state delegate a GameSessionService, trasformando questo controller in un puro
 * orchestratore di richieste e risposte HTTP (livello di trasporto).
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping("/api/v1/sessions/foosball")
public class FoosballSessionController {

    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Integer> payload) {
        Integer tableId = payload.get("table_id");
        if (tableId == null) {
            return ResponseEntity.badRequest().body("Il campo 'table_id' è obbligatorio nel JSON.");
        }

        // Estrazione username dal Security Context (basato su JWT stateless)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            // Delega al service la logica di validazione, controllo credito, persistenza ed emissione eventi MQTT
            GameSessionDTO dto = gameSessionService.startSession(username, tableId);

            // Costruzione risposta HATEOAS
            EntityModel<GameSessionDTO> entityModel = EntityModel.of(dto);
            
            // Link "self"
            entityModel.add(linkTo(methodOn(FoosballSessionController.class).getCurrentSession()).withSelfRel());
            
            // Link "force-stop" per gli admin
            entityModel.add(Link.of("/api/v1/admin/sessions/" + dto.getId() + "/force-stop").withRel("force-stop"));
            
            // Link "dashboard" per navigazione base
            entityModel.add(Link.of("/api/v1/dashboard").withRel("dashboard"));

            return ResponseEntity.ok(entityModel);

        } catch (IllegalArgumentException e) {
            // L'utente non esiste nel DB
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalStateException e) {
            // L'utente ha già una sessione attiva
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSession() {
        // Estrazione username dal Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Delega la lettura al service
        Optional<GameSessionDTO> sessionOpt = gameSessionService.getCurrentSession(username);
        
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessuna partita in corso per l'utente.");
        }

        // Mapping e HATEOAS
        EntityModel<GameSessionDTO> entityModel = EntityModel.of(sessionOpt.get());
        entityModel.add(linkTo(methodOn(FoosballSessionController.class).getCurrentSession()).withSelfRel());
        
        return ResponseEntity.ok(entityModel);
    }
}