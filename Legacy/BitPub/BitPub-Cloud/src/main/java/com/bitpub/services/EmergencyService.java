package com.bitpub.services;

import com.bitpub.mqtt.MqttAdminGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * EmergencyService - Gestore centralizzato delle operazioni critiche e di sicurezza.
 * * Nota Architetturale:
 * Astrae la comunicazione hardware dal layer API. Centralizza i comandi di sblocco
 * forzato (Emergency Unlock) permettendo di aggiungere facilmente logging di sistema
 * o verifiche di stato aggiuntive prima dell'invio del comando al broker.
 * @author Stefano Bellan 20054330
 */
@Service
public class EmergencyService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyService.class);
    private final MqttAdminGateway mqttAdminGateway;

    @Autowired
    public EmergencyService(MqttAdminGateway mqttAdminGateway) {
        this.mqttAdminGateway = mqttAdminGateway;
    }

    /**
     * Invia un comando di sblocco forzato a un tavolo specifico.
     * Viene utilizzato in situazioni dove il flusso normale di gioco è bloccato
     * o per interventi tecnici immediati.
     * * @param localeId Identificativo del locale (Edge Node).
     * @param tavoloId Identificativo del tavolo fisico.
     */
    public void forceUnlockTable(Long localeId, String tavoloId) {
        logger.warn("ATTIVAZIONE SBLOCCO EMERGENZA - Locale: {}, Tavolo: {}", localeId, tavoloId);
        
        // Costruzione del topic secondo le convenzioni del progetto
        String topic = String.format("bitpub/locale/%d/tavolo/%s/admin/command", localeId, tavoloId);
        
        // Invio del comando grezzo tramite il gateway hardware
        mqttAdminGateway.publish(topic, "FORCE_UNLOCK");
    }
}