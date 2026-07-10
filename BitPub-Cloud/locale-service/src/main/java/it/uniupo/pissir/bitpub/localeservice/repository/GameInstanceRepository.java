package it.uniupo.pissir.bitpub.localeservice.repository;

import it.uniupo.pissir.bitpub.localeservice.domain.GameInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Interfaccia per l'accesso ai dati delle istanze di gioco. 
 * Estende JpaRepository per fornire i metodi standard di interrogazione e salvataggio sul database.
 */
@Repository
public interface GameInstanceRepository extends JpaRepository<GameInstance, String> {
    List<GameInstance> findByLocaleId(String localeId);
    Optional<GameInstance> findByLocaleIdAndLocalInstanceId(String localeId, String localInstanceId);
}
