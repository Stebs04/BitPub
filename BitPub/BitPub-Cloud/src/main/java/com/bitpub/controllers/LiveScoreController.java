package com.bitpub.controllers;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.models.GameSessionEntity;
import com.bitpub.models.Utente;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.repository.UtenteRepository;
import com.bitpub.services.ElaborazioneEventiService;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/active")
@PreAuthorize("isAuthenticated()")
public class LiveScoreController {

    @Autowired
    private ElaborazioneEventiService elaborazioneEventiService;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getActiveSessionScore(@PathVariable Long sessionId) {
        GameSessionEntity session = gameSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        JsonObject response = new JsonObject();
        response.addProperty("status", session.getStatus());

        Utente player = utenteRepository.findById(session.getUserId()).orElse(null);
        response.addProperty("player1", player != null ? player.getUsername() : "Player 1");
        response.addProperty("player2", "Simulatore");

        Object scoreObj = elaborazioneEventiService.getLiveScore(sessionId);
        if (scoreObj instanceof PartitaCalciobalilla) {
            PartitaCalciobalilla pc = (PartitaCalciobalilla) scoreObj;
            response.addProperty("score1", String.valueOf(pc.getGoalRossi()));
            response.addProperty("score2", String.valueOf(pc.getGoalBlu()));
        } else {
            response.addProperty("score1", "0");
            response.addProperty("score2", "0");
        }

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response.toString());
    }
}
