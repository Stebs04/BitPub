/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Interfaccia per l'accesso ai dati delle squadre memorizzate nel database.
 * Eredita i metodi base per le operazioni CRUD da JpaRepository.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
}
