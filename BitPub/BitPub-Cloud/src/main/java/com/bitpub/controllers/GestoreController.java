/**
 * Controller per le funzionalità riservate al GESTORE.
 * Gestisce il monitoraggio real-time, le statistiche aggregate e i tornei.
 * * @author Stefano Bellan
 */
package com.bitpub.controllers;

import com.bitpub.models.*;
import com.bitpub.repository.*;
import com.bitpub.assembler.TorneoModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/gestore")
@PreAuthorize("hasRole('GESTORE')")
public class GestoreController {

    @Autowired private TorneoRepository torneoRepository;
    @Autowired private MqttLogRepository mqttLogRepository;
    @Autowired private PartitaCalciobalillaRepository calciobalillaRepo;
    @Autowired private PartitaFreccetteRepository freccetteRepo;
    @Autowired private PartitaBiliardoRepository biliardoRepo;
    @Autowired private TorneoModelAssembler torneoAssembler;

    /**
     * Endpoint per il monitoraggio real-time dei dispositivi nel locale.
     * Determina se una macchina è 'attiva' basandosi sui log MQTT degli ultimi 60 secondi.
     */
    @GetMapping("/locali/{localeId}/macchine")
    public ResponseEntity<?> getMacchineAttive(@PathVariable Long localeId) {
        // Definiamo una finestra temporale per considerare la macchina "online"
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(60);
        
        // Recuperiamo i seriali che hanno inviato log recentemente
        List<String> serialiAttivi = mqttLogRepository.findDistinctSerialiByLocaleAndTimestampAfter(localeId, threshold);
        
        return ResponseEntity.ok(serialiAttivi);
    }

    /**
     * Recupera tutte le partite attualmente in corso nel locale, unendo i vari tipi di gioco.
     */
    @GetMapping("/locali/{localeId}/partite/attive")
    public ResponseEntity<?> getPartiteInCorso(@PathVariable Long localeId) {
        List<Object> tutteLePartiteInCorso = new ArrayList<>();
        
        tutteLePartiteInCorso.addAll(calciobalillaRepo.findByLocaleIdAndStato(localeId, "IN_CORSO"));
        tutteLePartiteInCorso.addAll(freccetteRepo.findByLocaleIdAndStato(localeId, "IN_CORSO"));
        tutteLePartiteInCorso.addAll(biliardoRepo.findByLocaleIdAndStato(localeId, "IN_CORSO"));
        
        return ResponseEntity.ok(tutteLePartiteInCorso);
    }

    /**
     * Elabora statistiche aggregate per il gestore del locale.
     */
    @GetMapping("/locali/{localeId}/statistiche")
    public ResponseEntity<?> getStatistiche(@PathVariable Long localeId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        
        // 1. Conteggio partite oggi per ogni tipo
        long cbToday = calciobalillaRepo.countToday(localeId, startOfToday);
        long frToday = freccetteRepo.countToday(localeId, startOfToday);
        long biToday = biliardoRepo.countToday(localeId, startOfToday);

        // 2. Medie durate (gestendo i valori null se non ci sono partite)
        Double avgCb = Optional.ofNullable(calciobalillaRepo.calculateAverageDuration(localeId)).orElse(0.0);
        Double avgFr = Optional.ofNullable(freccetteRepo.calculateAverageDuration(localeId)).orElse(0.0);
        Double avgBi = Optional.ofNullable(biliardoRepo.calculateAverageDuration(localeId)).orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalePartiteOggi", cbToday + frToday + biToday);
        
        Map<String, Long> distribuzione = new HashMap<>();
        distribuzione.put("CALCIOBALILLA", cbToday);
        distribuzione.put("FRECCETTE", frToday);
        distribuzione.put("BILIARDO", biToday);
        stats.put("distribuzione", distribuzione);
        
        stats.put("mediaDurataMinuti", (avgCb + avgFr + avgBi) / 3.0); // Media semplice delle medie

        return ResponseEntity.ok(stats);
    }

    /**
     * Registra un nuovo torneo nel sistema.
     */
    @PostMapping("/tornei")
    public ResponseEntity<?> creaTorneo(@RequestBody Torneo nuovoTorneo) {
        Torneo salvato = torneoRepository.save(nuovoTorneo);
        // Restituiamo l'oggetto con i link HATEOAS tramite l'assembler
        return ResponseEntity.ok(torneoAssembler.toModel(salvato));
    }
}