package com.bitpub.repository;

import com.bitpub.models.MqttLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Timothy: Questo è il tuo DAO Layer.
 * Estendendo JpaRepository, Spring genera automaticamente il codice per salvare su DB.
 */
@Repository
public interface MqttLogRepository extends JpaRepository<MqttLog, Long> {
    // Qui potresti aggiungere metodi personalizzati, es:
    // List<MqttLog> findByTopic(String topic);
}