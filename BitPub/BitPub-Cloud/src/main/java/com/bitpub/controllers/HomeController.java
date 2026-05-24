package com.bitpub.controllers;

import com.bitpub.models.ResourceModel;
import com.bitpub.models.Utente;
import com.bitpub.dto.LocaleDTO;
import com.bitpub.services.UtenteService;
import com.bitpub.services.LocaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HomeController - Root endpoint API per la navigazione HATEOAS.
 */
@RestController
@RequestMapping("/api/v1/home")
@CrossOrigin(origins = "*")
public class HomeController {

    private final UtenteService utenteService;
    private final LocaleService localeService;

    @Autowired
    public HomeController(UtenteService utenteService, LocaleService localeService) {
        this.utenteService = utenteService;
        this.localeService = localeService;
    }

    @GetMapping
    public ResponseEntity<ResourceModel> getHomeLinks(Authentication authentication) {
        ResourceModel root = new ResourceModel();

        // Link statici di base
        root.addLink("login", "http://localhost:8080/api/v1/auth/login");
        root.addLink("register", "http://localhost:8080/api/v1/auth/register");
        root.addLink("locali", "http://localhost:8080/api/v1/locali");
        root.addLink("users", "http://localhost:8080/api/v1/utenti");
        root.addLink("me", "http://localhost:8080/api/v1/utenti/me");

        // Game and System Services
        root.addLink("foosball-start", "http://localhost:8080/api/v1/sessions/foosball/start");
        root.addLink("foosball-current", "http://localhost:8080/api/v1/sessions/foosball/current");
        root.addLink("sessions", "http://localhost:8080/api/v1/sessions/foosball");
        root.addLink("foosball-sessions", "http://localhost:8080/api/v1/sessions/foosball");
        root.addLink("tornei-calciobalilla", "http://localhost:8080/api/v1/tornei?game=FOOSBALL");
        root.addLink("billiards-stats", "http://localhost:8080/api/v1/biliardo/stats/me");
        root.addLink("freccette-stats", "http://localhost:8080/api/v1/freccette/stats/me");

        // Admin and System Monitoring
        root.addLink("system-logs", "http://localhost:8080/api/v1/system/logs");
        root.addLink("network-status", "http://localhost:8080/api/v1/system/network-status");
        root.addLink("edge-status", "http://localhost:8080/api/v1/admin/system/edge-status");
        root.addLink("active-sessions", "http://localhost:8080/api/v1/admin/sessions/active");

        // --- COSTRUZIONE DINAMICA LINK GESTORE ---
        root.addLink("gestore-partite-attive", "http://localhost:8080/api/v1/gestore/partite");
        root.addLink("tornei", "http://localhost:8080/api/v1/tornei");

        // Fallback URLs
        // Fallback URLs
        String statsUrl = "http://localhost:8080/api/v1/gestore/stats";
        String macchineUrl = "http://localhost:8080/api/v1/gestore/macchine";

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Utente user = utenteService.findByUsername(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Utente non trovato"));

                List<LocaleDTO> localiGestore = localeService.getLocaliByGestoreId(user.getId());

                if (!localiGestore.isEmpty()) {
                    Long idLocaleDinmico = localiGestore.get(0).getId();
                    statsUrl = "http://localhost:8080/api/v1/gestore/locale/" + idLocaleDinmico + "/statistiche";
                    macchineUrl = "http://localhost:8080/api/v1/gestore/locale/" + idLocaleDinmico + "/macchine";
                }
            } catch (Exception e) {
                System.err.println("[HomeController] Errore nel calcolo dei link: " + e.getMessage());
            }
        }

        // Aggiungiamo i link generati dinamicamente all'oggetto HATEOAS
        root.addLink("gestore-statistiche", statsUrl);
        root.addLink("gestore-macchine", macchineUrl);

        return ResponseEntity.ok(root);
    }
}

