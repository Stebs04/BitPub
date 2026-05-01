package com.bitpub.edge;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Listener dedicato all'intercettazione dei comandi amministrativi remoti provenienti dal Cloud.
 * Gestisce operazioni critiche come lo sblocco forzato delle risorse fisiche (tavoli, postazioni)
 * in caso di emergenza o anomalie di sessione.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class AdminCommandListener implements IMqttMessageListener {

    /**
     * Invocata quando un messaggio amministrativo viene ricevuto sul topic sottoscritto.
     * Analizza il payload per determinare l'azione correttiva da intraprendere.
     *
     * @param topic   Il topic MQTT su cui è stato pubblicato il comando.
     * @param message Il contenuto del comando (Payload).
     * @throws Exception In caso di errori durante l'elaborazione del comando.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Conversione del payload binario in stringa per l'analisi testuale
        String payload = new String(message.getPayload());
        System.out.println("[Edge] Comando ricevuto su " + topic + ": " + payload);

        // Verifica della presenza della direttiva di sblocco forzato nel messaggio
        if (payload.contains("FORCE_UNLOCK")) {
            eseguiSbloccoForzato(topic);
        }
    }

    /**
     * Esegue la procedura di sblocco hardware/software della risorsa locale.
     * Interrompe i timer attivi, resetta lo stato del simulatore e libera la risorsa.
     *
     * @param topic Il riferimento al canale che ha originato la richiesta (per identificare la risorsa).
     */
    private void eseguiSbloccoForzato(String topic) {
        // Logging dell'operazione critica per audit trail locale
        System.out.println("[Edge] !!! ESECUZIONE SBLOCCO FORZATO PER: " + topic);

        // TODO: Integrare il riferimento ai simulatori fisici (es. SimBiliardo.reset())
        // Logica prevista:
        // 1. Interruzione del task pianificato (Future.cancel)
        // 2. Reset dello stato della risorsa (ResourceState.FREE)
        // 3. Invio notifica di avvenuto sblocco al Cloud
    }
}
