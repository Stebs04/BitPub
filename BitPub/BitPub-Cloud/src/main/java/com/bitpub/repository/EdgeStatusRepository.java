package com.bitpub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bitpub.repository.EdgeStatusEntity;

/**
 * Repository per la gestione della persistenza degli stati dei nodi Edge.
 * Utilizza l'ID del locale come chiave primaria di tipo {@link String}.
 * Spring Data JPA fornisce automaticamente le implementazioni per le operazioni
 * di salvataggio, aggiornamento e recupero dei dati.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Repository
public interface EdgeStatusRepository extends JpaRepository<EdgeStatusEntity, String> {
    // La logica di aggiornamento (save) viene gestita in modo trasparente dal framework
    // integrandosi con il sistema di monitoraggio MQTT.
}
