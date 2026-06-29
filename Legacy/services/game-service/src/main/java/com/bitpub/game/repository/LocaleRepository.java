package com.bitpub.game.repository;

import com.bitpub.game.model.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocaleRepository extends JpaRepository<Locale, UUID> {
}
