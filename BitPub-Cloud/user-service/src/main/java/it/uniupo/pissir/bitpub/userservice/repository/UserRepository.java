package it.uniupo.pissir.bitpub.userservice.repository;

import it.uniupo.pissir.bitpub.userservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Interfaccia di accesso ai dati per le entità utente tramite Spring Data JPA.
 * Definisce i metodi necessari per le query sul database PostgreSQL sottostante.
 */

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
