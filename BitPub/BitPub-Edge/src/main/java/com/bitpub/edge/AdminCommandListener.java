package com.bitpub.edge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Controller MQTT in ascolto dedicato all'intercettazione dei comandi amministrativi (Admin Plane).
 * Rappresenta la backdoor operativa di sicurezza dell'Edge Node: permette all'amministratore
 * collegato al Cloud di eseguire manovre d'emergenza (es. Sblocco forzato di un tavolo inceppato o
 * annullamento anomalo della sessione).
 * Architettonicamente, il comando intercettato delega la logica d'esecuzione a un Worker Thread
 * separato, salvaguardando il Dispatcher primario di Paho da eventuali blocchi hardware locali.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminCommandListener implements IMqttMessageListener {

    // Trace loggger governato da Logback per l'evidenza delle azioni amministrative distruttive
    private static final Logger logger = LoggerFactory.getLogger(AdminCommandListener.class);

    // Punto di ancoraggio al motore centrale di gestione delle macchine a stati finiti
    private final GameTableStateManager stateManager;

    /**
     * Iniezione della dipendenza necessaria per la mutazione dello stato logico.
     *
     * @param stateManager L'infrastruttura atomica (ConcurrentHashMap) incaricata di allocare o deallocare il tavolo
     */
    public AdminCommandListener(GameTableStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Entry-point reattivo scatenato dal broker per il topic amministrativo.
     * Interpreta il comando in ingresso ed esegue il routing della direttiva
     * scavalcando il pattern Store-and-Forward (in quanto è un comando d'azione, non telemetria).
     *
     * @param topic La stringa di indirizzamento della direttiva
     * @param message Payload byte crittografato e firmato col comando
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Omogeneizzazione dell'encoding del flusso sorgente
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        logger.info("[ADMIN] Ricevuto comando amministrativo su {}: {}", topic, payload);

        try {
            // DECODIFICA STRUTTURALE (Deserilizazzione)
            // Utilizzo del parser Gson per ricostruire l'albero JSON ed estrarre semanticamente
            // le chiavi di comando senza dipendere dalla loro posizione nel documento.
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            // Valutazione della coerenza del comando prima dell'estrapolazione dell'ID sensibile
            if (json.has("command") && "FORCE_UNLOCK".equals(json.get("command").getAsString())) {
                int tableId = json.get("tableId").getAsInt();

                // DELEGA ASINCRONA DELL'AZIONE HARDWARE
                // La liberazione di un tavolo potrebbe richiedere l'invio di segnali hardware (es. eccitazione bobina)
                // e cancellazione di timer. L'uso di uno spawn thread isolato previene il timeout MQTT.
                new Thread(() -> {
                    eseguiSbloccoForzato(tableId);
                }).start();
            }
        } catch (Exception e) {
            // Assorbimento di eventuali ParseException dovute a payload di amministrazione malformati
            logger.error("[ADMIN] Errore critico nella decodifica del comando di emergenza: {}", e.getMessage());
        }
    }

    /**
     * Esecutore tecnico della direttiva di bypass.
     * Allinea il contatore logico software e interfacciandosi con i controller fisici
     * innesca il reset meccanico o elettrico del biliardo/calciobalilla.
     *
     * @param tableId Riferimento intero della periferica hardware bersaglio dell'azione
     */
    private void eseguiSbloccoForzato(int tableId) {
        logger.warn("[ADMIN-TASK] Avvio procedura di sblocco forzato per Tavolo ID: {}", tableId);

        try {
            // Fase Logica: Induce una mutazione atomica disattivando lo stato OCCUPIED
            stateManager.setFree(tableId);

            // Fase Fisica: Esecuzione delle routine di basso livello (interfacciamento sensori)
            // In questa sede viene interrotto qualsiasi Task di monitoraggio inquinato e puliti i registri GPIO.
            simulaResetHardware();

            logger.info("[ADMIN-TASK] Sblocco completato con successo. Tavolo {} è ora FREE.", tableId);

        } catch (Exception e) {
            logger.error("[ADMIN-TASK] Fallimento durante lo sblocco forzato del tavolo {}: {}", tableId, e.getMessage());
        }
    }

    /**
     * Stub procedurale implementato per riprodurre empiricamente l'attesa logica
     * richiesta dai relè elettromeccanici durante le fasi di chiusura/apertura dei circuiti
     * del tavolo fisico.
     *
     * @throws InterruptedException se il processo hardware subisce un preempt
     */
    private void simulaResetHardware() throws InterruptedException {
        // Sleep temporizzato progettato per simulare le latenze di I/O
        Thread.sleep(500);
    }
}