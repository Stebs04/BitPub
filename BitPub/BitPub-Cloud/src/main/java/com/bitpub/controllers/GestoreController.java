package com.bitpub.controllers;

import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST per le operazioni della dashboard GESTORE.
 * Gestisce il recupero delle statistiche, partite attive, macchine e la creazione di tornei.
 *
 * @author Stefano Bellan
 */
@RestController
@RequestMapping("/api/v1/gestore")
// Assicura che solo il ruolo GESTORE possa invocare questi endpoint
@PreAuthorize("hasAuthority('GESTORE')")
public class GestoreController {

    @Autowired
    private TorneoRepository torneoRepository;
    
    // NOTA: Qui dovresti iniettare anche i repository per Partite e Macchine (Dispositivi)
    // a seconda di come li hai nominati nel tuo modello dati.

    /**
     * Recupera la lista delle macchine (dispositivi fisici) attive in un locale.
     * @param localeId ID del locale
     * @return Lista di macchine con stato aggiornato real-time
     */
    @GetMapping("/locali/{localeId}/macchine")
    public ResponseEntity<?> getMacchineAttive(@PathVariable Long localeId) {
        // TODO: Sostituire con la vera query al DB. 
        // L'Edge Node aggiorna il campo 'attiva' tramite MQTT.
        List<Map<String, Object>> macchineMock = List.of(
            Map.of("id", 1, "nome", "Tavolo Calciobalilla 1", "tipoGioco", "CALCIOBALILLA", "attiva", true),
            Map.of("id", 2, "nome", "Bersaglio Freccette 1", "tipoGioco", "FRECCETTE", "attiva", false)
        );
        return ResponseEntity.ok(macchineMock);
    }

    /**
     * Recupera le partite attualmente in corso nel locale.
     * @param localeId ID del locale
     * @return Lista di partite con stato IN_CORSO
     */
    @GetMapping("/locali/{localeId}/partite/attive")
    public ResponseEntity<?> getPartiteAttive(@PathVariable Long localeId) {
        // TODO: Usare il repository per cercare le partite con stato "IN_CORSO" per questo locale
        return ResponseEntity.ok(List.of()); 
    }

    /**
     * Calcola le statistiche aggregate per il locale.
     * @param localeId ID del locale
     * @return Mappa contenente statistiche aggregate (partite oggi, conteggi, media durata)
     */
    @GetMapping("/locali/{localeId}/statistiche")
    public ResponseEntity<?> getStatistiche(@PathVariable Long localeId) {
        // Dati di esempio che andranno calcolati tramite query JPA
        Map<String, Object> stats = Map.of(
            "partiteOggi", 15,
            "calciobalilla", 8,
            "freccette", 5,
            "biliardo", 2,
            "mediaDurataMinuti", 12.5
        );
        return ResponseEntity.ok(stats);
    }

    /**
     * Crea un nuovo torneo nel sistema.
     * @param torneo Oggetto Torneo derivato dal body JSON
     * @return Il torneo appena creato
     */
    @PostMapping("/tornei")
    public ResponseEntity<?> creaTorneo(@RequestBody Torneo torneo) {
        Torneo salvato = torneoRepository.save(torneo);
        return ResponseEntity.ok(salvato);
    }
}