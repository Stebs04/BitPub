package it.uniupo.pissir.bitpub.localeservice.repository;

import it.uniupo.pissir.bitpub.localeservice.domain.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Repository per la gestione della persistenza dell'entita' Locale.
 */
@Repository
public interface LocaleRepository extends JpaRepository<Locale, String> {
    List<Locale> findByAdminId(String adminId);
}
