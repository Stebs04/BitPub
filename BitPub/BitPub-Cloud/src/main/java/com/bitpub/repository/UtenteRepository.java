package com.bitpub.repository;

import com.bitpub.models.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione della persistenza dell'entità {@link Utente}.
 * Estende JpaRepository per fornire l'accesso ai dati tramite operazioni CRUD standard,
 * gestione della paginazione e query derivate basate sulle proprietà del modello.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    /**
     * Ricerca un utente in base allo username univoco.
     *
     * @param username Lo username dell'utente (Subject del token JWT).
     * @return Un {@link Optional} contenente l'utente se presente nel database.
     */
    Optional<Utente> findByUsername(String username);

    /**
     * Ricerca un utente tramite l'indirizzo email registrato.
     * Utilizzato primariamente durante i processi di autenticazione e recupero credenziali.
     *
     * @param email L'indirizzo e-mail associato all'account.
     * @return Un {@link Optional} contenente il profilo utente corrispondente.
     */
    Optional<Utente> findByEmail(String email);

    /**
     * Verifica la disponibilità di uno username nel sistema.
     *
     * @param username Lo username da validare.
     * @return true se lo username è già occupato, false altrimenti.
     */
    boolean existsByUsername(String username);

    /**
     * Verifica se un indirizzo e-mail è già associato a un account esistente.
     *
     * @param email L'email da controllare.
     * @return true se l'email è presente nel database.
     */
    boolean existsByEmail(String email);

    /**
     * Filtra gli utenti in base al loro ruolo di accesso.
     *
     * @param role Il ruolo.
     * @return Una {@link List} di utenti appartenenti al ruolo specificato.
     */
    List<Utente> findByRole(Utente.Ruolo role);

    /**
     * Esegue una ricerca testuale parziale (fuzzy search) e case-insensitive su più campi.
     * La ricerca analizza nome, cognome e username per supportare i filtri della dashboard Admin.
     *
     * @param keyword La chiave di ricerca inserita dall'amministratore.
     * @return Una collezione di utenti che soddisfano i criteri di match parziale.
     */
    @Query("SELECT u FROM Utente u WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.cognome) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Utente> cercaPerNomeCognomeOUsername(@Param("keyword") String keyword);
}