package com.bitpub.services;

import com.bitpub.models.MqttLog;
import com.bitpub.repository.MqttLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistenceService {

    @Autowired
    private MqttLogRepository logRepository;

    /**
     * Luca: L'annotazione @Transactional garantisce la "blindatura" del salvataggio.
     * Se arrivano 100 messaggi insieme, Spring gestisce le code di scrittura al DB.
     */
    @Transactional
    public void salvaMessaggio(String topic, String payload) {
        MqttLog nuovoLog = new MqttLog(topic, payload);
        logRepository.save(nuovoLog);
        System.out.println("[CLOUD DB] Log salvato correttamente: " + topic);
    }
}