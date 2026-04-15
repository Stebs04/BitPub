package com.bitpub.repository;

import com.bitpub.models.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TorneoRepository extends JpaRepository<Torneo, Long> {
    // Spring crea automaticamente i metodi per salvare, cercare e cancellare!
}