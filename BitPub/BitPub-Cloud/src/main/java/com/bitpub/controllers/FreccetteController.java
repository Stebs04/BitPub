package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.repository.PartitaFreccetteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// Mappiamo questo controller sull'URL base che il nostro test sta cercando
@RequestMapping("/api/v1/freccette")
public class FreccetteController {

    @Autowired
    private PartitaFreccetteRepository freccetteRepository;

    // Creiamo il metodo per l'endpoint "/partite"
    @GetMapping("/partite")
    public ResponseEntity<List<PartitaFreccette>> getAllPartite() {

        // 1. Peschiamo tutte le partite dal database
        List<PartitaFreccette> partite = freccetteRepository.findAll();

        // 2. Le restituiamo con stato HTTP 200 (OK).
        // Spring si occuperà automaticamente di trasformarle in JSON!
        return ResponseEntity.ok(partite);
    }
}
