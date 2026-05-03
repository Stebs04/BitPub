package com.bitpub.controllers;

import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione amministrativa dell'anagrafica utenti.
 * Espone gli endpoint per il monitoraggio, il filtraggio e il controllo degli accessi
 * all'interno dell'ecosistema BitPub, con restrizioni di accesso basate sui ruoli.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*") // Abilita le chiamate cross-origin per il client JavaFX
public class AdminUserController {

    /** Repository per le operazioni di persistenza sugli utenti. */
    @Autowired
    private UtenteRepository utenteRepository;

    /**
     * Recupera una collezione di utenti applicando filtri condizionali.
     * Consente la ricerca per ruolo specifico o tramite stringa di ricerca parziale.
     * Accesso limitato agli utenti con ruolo 'ADMIN'.
     *
     * @param role   Filtro opzionale per il ruolo (es. "GESTORE").
     * @param search Parola chiave opzionale per ricerca su nome, cognome o username.
     * @return {@link ResponseEntity} contenente la lista degli utenti filtrati.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utente>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {

        // Logica di selezione: prioritizzazione del filtro per ruolo
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(utenteRepository.findByRole(role));
        }

        // Utilizzo della ricerca full-text parziale se fornita una stringa di ricerca
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(utenteRepository.cercaPerNomeCognomeOUsername(search));
        }

        // Restituzione dell'intero set di utenti in assenza di parametri di filtraggio
        return ResponseEntity.ok(utenteRepository.findAll());
    }

    /**
     * Inverte lo stato di operatività (Attivo/Sospeso) di un determinato account.
     * L'operazione è identificata tramite lo username univoco dell'utente.
     *
     * @param username Lo username dell'utente su cui intervenire.
     * @return ResponseEntity con stato 200 (OK) se aggiornato, o 404 (Not Found) se inesistente.
     */
    @PutMapping("/{username}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable String username) {
        // Ricerca atomica e aggiornamento dello stato booleano
        return utenteRepository.findByUsername(username)
                .map(utente -> {
                    // Inversione logica dello stato attuale
                    utente.setAttivo(!utente.isAttivo());
                    utenteRepository.save(utente);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Alterna il ruolo dell'utente tra {@code USER} e {@code GESTORE}.
     * Se l'utente ha ruolo {@code USER} viene promosso a {@code GESTORE}; se è già
     * {@code GESTORE} viene retrocesso a {@code USER}. Gli account {@code ADMIN} sono
     * esclusi dalla modifica per sicurezza.
     *
     * @param username Lo username univoco dell'utente da modificare.
     * @return {@link ResponseEntity} 200 OK con il ruolo aggiornato, o 404 se non trovato,
     *         o 400 se si tenta di modificare un account ADMIN.
     */
    @PutMapping("/{username}/toggle-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserRole(@PathVariable String username) {
        return utenteRepository.findByUsername(username)
                .map(utente -> {
                    // Protezione: il ruolo ADMIN non può essere modificato tramite questa API
                    if ("ADMIN".equalsIgnoreCase(utente.getRole())) {
                        return ResponseEntity.badRequest().body("Impossibile modificare il ruolo di un ADMIN.");
                    }
                    // Toggle tra USER e GESTORE
                    String nuovoRuolo = "GESTORE".equalsIgnoreCase(utente.getRole()) ? "USER" : "GESTORE";
                    utente.setRole(nuovoRuolo);
                    utenteRepository.save(utente);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
