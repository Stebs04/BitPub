/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.repository;

import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorDefinitionRepository extends JpaRepository<SensorDefinition, String> {
    List<SensorDefinition> findByGameTypeId(String gameTypeId);
}
