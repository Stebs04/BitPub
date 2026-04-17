package com.bitpub.repository; // Definizione del package di appartenenza

import com.bitpub.models.Utente; // Entity di riferimento gestita dal repository
import org.springframework.data.jpa.repository.JpaRepository; // Interfaccia base per operazioni CRUD e paginazione
import org.springframework.data.jpa.repository.Query; // Annotation per definire query JPQL custom
import org.springframework.data.repository.query.Param; // Per il mapping sicuro dei parametri nelle query
import org.springframework.stereotype.Repository; // Annotation di stereotipo per il discovery di Spring
import java.util.List; // Struttura dati per collezioni di risultati
import java.util.Optional; // Wrapper per la gestione sicura dei valori potenzialmente nulli

/**
 * Repository per la gestione della persistenza dell'entità {@link Utente}.
 * Estende JpaRepository per ereditare i metodi standard di salvataggio, eliminazione e ricerca.
 * @author Stefano Bellan 20054330
 */
@Repository // Marca l'interfaccia come componente Spring Data e abilita la traduzione delle eccezioni DB
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    /**
     * Ricerca un utente in base al nickname.
     * @param nickname il nickname univoco dell'utente
     * @return un Optional contenente l'utente se trovato, altrimenti vuoto
     */
    Optional<Utente> findByNickname(String nickname); // Query derivata: SELECT * FROM utente WHERE nickname = ?

    /**
     * Ricerca un utente in base all'indirizzo email.
     * @param email l'email associata all'account
     * @return un Optional per una gestione null-safe dei risultati
     */
    Optional<Utente> findByEmail(String email); // Query derivata: utile per il caricamento dell'utente in fase di login

    /**
     * Verifica la presenza di un utente con il nickname specificato.
     * @param nickname il nickname da controllare
     * @return true se esiste già, false altrimenti
     */
    boolean existsByNickname(String nickname); // Ottimizzato: esegue una SELECT COUNT o EXISTS a livello DB

    /**
     * Verifica la presenza di un utente con l'email specificata.
     * @param email l'indirizzo email da controllare
     * @return true se l'email è già registrata
     */
    boolean existsByEmail(String email); // Fondamentale per la validazione in fase di registrazione (Unique constraint)

    /**
     * Recupera tutti gli utenti associati a un determinato ruolo.
     * @param ruolo il nome del ruolo (es. ROLE_USER, ROLE_ADMIN)
     * @return una lista di utenti filtrati
     */
    List<Utente> findByRuolo(String ruolo); // Query derivata: SELECT * FROM utente WHERE ruolo = ?

    /**
     * Esegue una ricerca full-text parziale e case-insensitive su nome e cognome.
     * @param keyword la stringa da ricercare
     * @return lista di utenti che corrispondono ai criteri di ricerca
     */
    @Query("SELECT u FROM Utente u WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.cognome) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Utente> cercaPerNomeOCognome(@Param("keyword") String keyword); // JPQL custom con concatenazione di wildcard per la clausola LIKE
}