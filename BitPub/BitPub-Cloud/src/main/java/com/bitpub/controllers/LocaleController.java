/**
 * Package che espone gli endpoint REST per il sistema BitPub.
 */
package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.repository.LocaleRepository;
import com.bitpub.utils.HateoasResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller per la gestione del ciclo di vita dell'entità {@link Locale}.
 * Espone le API per operazioni CRUD integrando il supporto HATEOAS per la navigazione delle risorse.
 */
@RestController // Marca la classe come controller REST (risposte in JSON di default)
@RequestMapping(value = "/api/locali", produces = "application/resources.v1+json") // Base path per tutti gli endpoint di questo controller
public class LocaleController {

    @Autowired // Injection del repository tramite il container di Spring
    private LocaleRepository localeRepository;

    // --- METODI DI LETTURA (GET) ---

    /**
     * Recupera la lista completa di tutti i locali.
     * @return Lista di risorse avvolte in HateoasResource per includere i link.
     */
    @GetMapping // Mapping per richieste GET su /api/locali
    public ResponseEntity<List<HateoasResource<Locale>>> getAllLocali() {
        return ResponseEntity.ok(
                localeRepository.findAll().stream() // Stream per trasformare la lista in risorse HATEOAS
                        .map(this::iniettaLink) // Arricchisce ogni oggetto con i metadati di navigazione
                        .collect(Collectors.toList())
        );
    }

    /**
     * Recupera un singolo locale tramite il suo ID univoco.
     * @param id Identificativo del locale.
     * @return 200 OK con la risorsa, oppure 404 Not Found se inesistente.
     */
    @GetMapping("/{id}") // Path variable per ID dinamico
    public ResponseEntity<HateoasResource<Locale>> getById(@PathVariable Long id) {
        return localeRepository.findById(id)
                .map(l -> ResponseEntity.ok(iniettaLink(l))) // Mapping funzionale del risultato
                .orElse(ResponseEntity.notFound().build()); // Gestione elegante del caso nullo
    }

    /**
     * Recupera un locale ricercandolo per nome esatto.
     * @param nome Stringa del nome da cercare.
     * @return La risorsa trovata o errore 404.
     */
    @GetMapping("/nome/{nome}")
    public ResponseEntity<HateoasResource<Locale>> getByNome(@PathVariable String nome) {
        return localeRepository.findByName(nome)
                .map(l -> ResponseEntity.ok(iniettaLink(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    // --- METODI DI SCRITTURA E MODIFICA (POST, PUT, DELETE) ---

    /**
     * Crea un nuovo locale previa validazione dell'unicità di Nome ed IP.
     * @param nuovo DTO del locale ricevuto nel corpo della request.
     * @return 201 Created con la risorsa salvata o 409 Conflict in caso di duplicati.
     */
    @PostMapping // Mapping per creazione nuova risorsa
    public ResponseEntity<?> creaLocale(@RequestBody Locale nuovo) {
        // Business logic di validazione: prevenzione duplicati lato applicativo
        if (localeRepository.existsByName(nuovo.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Nome locale già esistente.");
        }
        if (localeRepository.existsByIpAddressEdge(nuovo.getIpAddressEdge())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("IP Edge già assegnato a un altro locale.");
        }
        Locale salvato = localeRepository.save(nuovo); // Persistenza su database
        return ResponseEntity.status(HttpStatus.CREATED).body(iniettaLink(salvato));
    }

    /**
     * Aggiorna i dati di un locale esistente.
     * @param id ID del locale da modificare.
     * @param datiAggiornati Nuovi valori da impostare.
     * @return 200 OK con la risorsa aggiornata o 404 se non trovata.
     */
    @PutMapping("/{id}") // Mapping per aggiornamento totale/idempotente
    public ResponseEntity<?> aggiornaLocale(@PathVariable Long id, @RequestBody Locale datiAggiornati) {
        return localeRepository.findById(id).map(esistente -> {
            esistente.setName(datiAggiornati.getName()); // Update del campo Nome
            esistente.setIpAddressEdge(datiAggiornati.getIpAddressEdge()); // Update del campo IP
            localeRepository.save(esistente); // Save/Update gestito da JPA
            return ResponseEntity.ok(iniettaLink(esistente));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un locale dal sistema.
     * @param id ID della risorsa da rimuovere.
     * @return 204 No Content se l'operazione ha successo.
     */
    @DeleteMapping("/{id}") // Mapping per eliminazione
    public ResponseEntity<Void> eliminaLocale(@PathVariable Long id) {
        if (localeRepository.existsById(id)) {
            localeRepository.deleteById(id);
            return ResponseEntity.noContent().build(); // Standard REST: risposta vuota su successo
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Metodo helper privato per iniettare i link HATEOAS (Self e Dispositivi).
     * @param locale L'entity da avvolgere.
     * @return La risorsa arricchita di hypermedia.
     */
    private HateoasResource<Locale> iniettaLink(Locale locale) {
        HateoasResource<Locale> r = new HateoasResource<>(locale);
        r.addLink("self", "/api/locali/" + locale.getId()); // Link alla risorsa stessa
        r.addLink("dispositivi", "/api/locali/" + locale.getId() + "/dispositivi"); // Link alla relazione collegata
        return r;
    }
}
