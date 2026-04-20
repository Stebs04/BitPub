package com.bitpub.controllers;

import com.bitpub.models.CalciobalillaStats;
import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import com.bitpub.utils.HateoasResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller per la gestione delle risorse relative alle partite di calciobalilla.
 * Espone gli endpoint per il recupero delle singole partite (HATEOAS) e per il
 * calcolo delle statistiche aggregate globali.
 *
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping(value = "/api/calciobalilla", produces = "application/resources.v1+json")
public class CalciobalillaController {

    @Autowired
    private PartitaCalciobalillaRepository repository;

    /**
     * Recupera l'elenco completo delle partite registrate nel sistema.
     * Ogni risorsa restituita è arricchita con i link ipertestuali necessari.
     *
     * @return ResponseEntity contenente una lista di {@link HateoasResource} di partite.
     */
    @GetMapping
    public ResponseEntity<List<HateoasResource<PartitaCalciobalilla>>> getAllEvents() {
        // Recupera le entità dal database e le trasforma in risorse HATEOAS tramite Stream API
        List<HateoasResource<PartitaCalciobalilla>> risorse = repository.findAll().stream()
                .map(this::aggiungiLinkHateoas)
                .collect(Collectors.toList());

        return ResponseEntity.ok(risorse);
    }

    /**
     * Recupera i dettagli di una specifica partita tramite il suo identificativo univoco.
     *
     * @param id L'ID della partita da cercare.
     * @return ResponseEntity con la risorsa trovata o status 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<HateoasResource<PartitaCalciobalilla>> getById(@PathVariable Long id) {
        // Utilizzo del pattern funzionale di Optional per gestire la presenza o assenza del dato
        return repository.findById(id)
                .map(p -> ResponseEntity.ok(aggiungiLinkHateoas(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Fornisce le statistiche globali aggregate di tutto il sistema Calciobalilla.
     * Raggruppa i dati su falli (rullate) e vittorie per squadra.
     *
     * @return ResponseEntity contenente l'oggetto {@link CalciobalillaStats}.
     */
    @GetMapping("/stats")
    public ResponseEntity<CalciobalillaStats> getStats() {
        // Gestione del potenziale null restituito da SUM() su un database vuoto
        int rullate = repository.countTotalRullate() != null ? repository.countTotalRullate() : 0;

        // Recupero dei conteggi distribuiti per squadra tramite query custom
        int vRossi = repository.countVittorieRossi();
        int vBlu = repository.countVittorieBlu();

        // Incapsulamento nel DTO specifico per la risposta JSON
        return ResponseEntity.ok(new CalciobalillaStats(rullate, vRossi, vBlu));
    }

    /**
     * Arricchisce l'entità PartitaCalciobalilla con i metadati ipertestuali (HATEOAS).
     * Questo metodo centralizza la logica di generazione dei link per mantenere la coerenza.
     *
     * @param partita L'entità core da mappare.
     * @return La risorsa arricchita con i link "self", "dettagli_squadra" e "prossimo_match".
     */
    private HateoasResource<PartitaCalciobalilla> aggiungiLinkHateoas(PartitaCalciobalilla partita) {
        HateoasResource<PartitaCalciobalilla> risorsa = new HateoasResource<>(partita);

        // Link obbligatorio HATEOAS: punta alla risorsa corrente
        risorsa.addLink("self", "/api/calciobalilla/" + partita.getId());

        /**
         * Iniezione link dinamici per l'integrazione tra moduli.
         * I link permettono al client di navigare verso i dettagli squadra e il calendario.
         */
        risorsa.addLink("dettagli_squadra", "/api/squadre/info/" + partita.getId());
        risorsa.addLink("prossimo_match", "/api/partite/prossime/" + partita.getId());

        return risorsa;
    }
}
