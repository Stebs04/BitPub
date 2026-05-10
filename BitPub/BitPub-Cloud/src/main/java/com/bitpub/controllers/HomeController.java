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
        return ResponseEntity.ok(root);
    }
}
