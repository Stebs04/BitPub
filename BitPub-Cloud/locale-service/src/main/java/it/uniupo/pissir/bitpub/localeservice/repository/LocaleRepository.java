package it.uniupo.pissir.bitpub.localeservice.repository;

import it.uniupo.pissir.bitpub.localeservice.domain.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocaleRepository extends JpaRepository<Locale, String> {
    List<Locale> findByAdminId(String adminId);
}
