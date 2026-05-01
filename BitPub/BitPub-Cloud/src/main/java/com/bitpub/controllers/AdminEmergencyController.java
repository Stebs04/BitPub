package com.bitpub.controllers;

import com.bitpub.mqtt.MqttAdminGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST specializzato nella gestione delle operazioni critiche di amministrazione.
 * Agisce come bridge tecnologico convertendo le richieste HTTP provenienti dalla Dashboard
 * in comandi di controllo per il protocollo MQTT diretti ai nodi Edge locali.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/admin/emergency")
@CrossOrigin(origins = "*") // Abilita l'interazione con client esterni (es: Dashboard JavaFX)
public class AdminEmergencyController {

    /** Gateway centralizzato per la comunicazione asincrona verso il broker MQTT. */
    @Autowired
    private MqttAdminGateway mqttGateway;

    /**
     * Endpoint dedicato allo sblocco forzato delle risorse fisiche (es. Tavoli da biliardo).
     * Invia un pacchetto di comando al nodo Edge specifico per risolvere stati di blocco hardware.
     *
     * Endpoint: POST /api/v1/admin/emergency/unlock/{venueId}/{tableId}
     * Sicurezza: Accesso ristretto esclusivamente agli utenti con privilegio 'ADMIN'.
     *
     * @param venueId L'identificativo univoco della sede (es. Milano-01).
     * @param tableId L'identificativo della risorsa locale da sbloccare.
     * @return ResponseEntity con lo stato dell'operazione e messaggio di conferma invio.
     */
    @PostMapping("/unlock/{venueId}/{tableId}")
    @PreAuthorize("hasRole('ADMIN')") // Intercettazione JWT per la verifica dei privilegi amministrativi
    public ResponseEntity<?> forceUnlock(@PathVariable String venueId, @PathVariable String tableId) {

        // Costruzione del topic gerarchico conforme alle specifiche di routing del sistema Edge
        String topic = "bitpub/locali/" + venueId + "/biliardo/" + tableId + "/cmd";

        // Pubblicazione del comando "FORCE_UNLOCK" tramite il gateway MQTT.
        // Nota tecnica: L'operazione utilizza il QoS 2 per garantire l'esecuzione del comando critico.
        mqttGateway.publish(topic, "FORCE_UNLOCK");

        // Risposta immediata al client JavaFX per segnalare l'avvenuta presa in carico della richiesta
        return ResponseEntity.ok("Comando di sblocco inviato con successo al locale: " + venueId);
    }
}
