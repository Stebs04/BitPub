package com.bitpub.repository;

import com.bitpub.models.MqttLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Timothy: Questo è il tuo DAO Layer.
 * Estendendo JpaRepository, Spring genera automaticamente il codice per salvare su DB.
 */
@Repository
public interface MqttLogRepository extends JpaRepository<MqttLog, Long> {
    
    // Qui potresti aggiungere metodi personalizzati, es:
    // List<MqttLog> findByTopic(String topic);

    /**
     * Recupera i seriali distinti associati a un determinato locale a partire da un timestamp specifico.
     * Implementazione JPQL ottimizzata per filtrare log MQTT in base alla struttura del topic.
     */
    @Query("SELECT DISTINCT m.topic FROM MqttLog m WHERE m.topic LIKE CONCAT('locali/', :localeId, '/%') AND m.timestamp >= :timestamp")
    List<String> findDistinctSerialiByLocaleAndTimestampAfter(@Param("localeId") Long localeId, @Param("timestamp") LocalDateTime timestamp);
}