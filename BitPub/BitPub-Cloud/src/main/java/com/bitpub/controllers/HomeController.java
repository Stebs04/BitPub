package com.bitpub.controllers;

import com.bitpub.models.ResourceModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HomeController - Root endpoint API per la navigazione HATEOAS.
 */
@RestController
@RequestMapping("/api/v1/home")
@CrossOrigin(origins = "*")
public class HomeController {

    @GetMapping
    public ResponseEntity<ResourceModel> getHomeLinks() {
        ResourceModel root = new ResourceModel();
        // L'URL di root espone i link essenziali per scoprire dinamicamente i servizi
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
        root.addLink("sessions", "http://localhost:8080/api/v1/admin/sessions/active"); 
        
        // Gestore Services
        root.addLink("gestore-macchine", "http://localhost:8080/api/v1/gestore/macchine");
        root.addLink("gestore-partite-attive", "http://localhost:8080/api/v1/gestore/partite");
        root.addLink("gestore-statistiche", "http://localhost:8080/api/v1/gestore/stats");
        root.addLink("tornei", "http://localhost:8080/api/v1/tornei");

        return ResponseEntity.ok(root);
    }
}
