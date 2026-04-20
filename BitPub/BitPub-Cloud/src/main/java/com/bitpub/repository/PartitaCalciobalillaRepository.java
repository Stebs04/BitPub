package com.bitpub.repository;

import com.bitpub.models.PartitaCalciobalilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per gestire le operazioni CRUD sulle partite di Calciobalilla.
 * @author Stefano Bellan 20054330
 */
@Repository
public interface PartitaCalciobalillaRepository extends JpaRepository<PartitaCalciobalilla, Long> {
}