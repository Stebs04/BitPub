/**
 * Package per i componenti di accesso ai dati del dominio BitPub.
 */
package com.bitpub.repository; // Definizione del package per la persistenza

import com.bitpub.models.Locale; // Entity Locale che rappresenta la tabella nel database
import org.springframework.data.jpa.repository.JpaRepository; // Interfaccia core per le operazioni CRUD standard
import org.springframework.stereotype.Repository; // Annotation per definire il bean come repository Spring
import java.util.Optional; // Container per prevenire ritorni nulli e gestire flussi funzionali

/**
 * Repository per la gestione della persistenza dell'entità {@link Locale}.
 * Sviluppato per la gestione dei nodi locali.
 * Estende JpaRepository fornendo l'astrazione sopra l'EntityManager.
 * @author Stefano Bellan 20054330
 */
@Repository // Registra il componente nel contesto Spring per l'Auto-Configuration
public interface LocaleRepository extends JpaRepository<Locale, Long> {

    /**
     * Recupera un Locale tramite il suo nome univoco.
     * @param name Nome identificativo del locale.
     * @return un Optional contenente il Locale se trovato, altrimenti Optional.empty().
     */
    Optional<Locale> findByName(String name); // Query derivata: SELECT * FROM locale WHERE name = ?

    /**
     * Verifica l'esistenza di un locale tramite il nome.
     * Utilizzato principalmente per validazioni in fase di inserimento.
     * @param name Nome del locale da verificare.
     * @return true se il nome è già presente a sistema.
     */
    boolean existsByName(String name); // Metodo di controllo rapido (SELECT COUNT)

    /**
     * Verifica se un indirizzo IP Edge è già associato a un locale esistente.
     * Cruciale per evitare conflitti di rete nella configurazione dei nodi.
     * @param ipAddressEdge L'indirizzo IP del nodo di confine.
     * @return true se l'IP è già registrato nel database.
     */
    boolean existsByIpAddressEdge(String ipAddressEdge); // Controllo di integrità per la rete dei nodi locali
}
