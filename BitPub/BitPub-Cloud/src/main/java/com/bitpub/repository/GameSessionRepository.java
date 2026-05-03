package com.bitpub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia Spring Data JPA per le operazioni CRUD sulla tabella game_session.
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSessionEntity, Long> {

    /**
     * Trova una sessione specifica di un utente che si trova in un determinato stato.
     * Utile per controllare se l'utente ha già una partita "IN_PROGRESS" prima di poterne avviare un'altra.
     *
     * @param userId l'ID logico dell'utente
     * @param status lo stato della sessione ("IN_PROGRESS", "FINISHED", ecc.)
     * @return un Optional contenente l'entità se presente
     */
    Optional<GameSessionEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * Recupera tutte le sessioni che si trovano in uno specifico stato.
     * Utilizzato primariamente dalla vista Admin per visualizzare l'elenco delle partite "IN_PROGRESS".
     *
     * @param status lo stato della sessione
     * @return una lista di GameSessionEntity
     */
    List<GameSessionEntity> findAllByStatus(String status);

    /**
     * Recupera una sessione per ID, garantendo però che appartenga all'utente richiesto.
     * È una query di sicurezza per impedire a un utente di accedere (tramite ID) ai dettagli di una partita altrui.
     *
     * @param id l'ID della sessione
     * @param userId l'ID dell'utente proprietario
     * @return un Optional contenente l'entità se il match è valido
     */
    Optional<GameSessionEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * Fallback: cerca una sessione attiva per tableId (usato quando sessionId non è nel payload MQTT).
     */
    Optional<GameSessionEntity> findByTableIdAndStatus(int tableId, String status);
}
