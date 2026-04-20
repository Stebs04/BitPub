package com.bitpub;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.utils.MqttCalciobalillaTopics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Simulatore software di un tavolo da calciobalilla IoT.
 * Gestisce l'intero ciclo di vita di una partita, simulando eventi fisici
 * (gol, rullate) e trasmettendo i dati in tempo reale via protocollo MQTT.
 *
 * @author Stefano Bellan 20054330
 */
public class SimCalciobalilla implements Runnable {

    private final String idLocale;
    private final String idDispositivo;
    private final String edgeBrokerUrl;
    private final Gson gson;
    private final Random random;

    /** Regola ufficiale per la conclusione del match */
    private final int MAX_GOL = 10;

    /** Flag di controllo per la terminazione sicura del thread */
    private volatile boolean inEsecuzione = true;

    /**
     * Inizializza il simulatore configurando i parametri di connessione e i parser.
     *
     * @param idLocale      Identificativo del bar/punto vendita.
     * @param idDispositivo Identificativo univoco del tavolo fisico.
     * @param edgeBrokerIp  Indirizzo IP dell'Edge Gateway locale.
     */
    public SimCalciobalilla(String idLocale, String idDispositivo, String edgeBrokerIp) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        this.edgeBrokerUrl = "tcp://" + edgeBrokerIp + ":1883";

        // GSON: Configurato per includere solo i campi annotati con @Expose per sicurezza
        this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        this.random = new Random();
    }

    /**
     * Arresta in modo pulito il loop di simulazione.
     */
    public void fermaSimulatore() {
        this.inEsecuzione = false;
    }

    /**
     * Entry point del thread. Gestisce la connessione MQTT e il loop infinito
     * di simulazione dei match.
     */
    @Override
    public void run() {
        try {
            // Configurazione client MQTT con ID univoco per evitare collisioni
            MqttClient client = new MqttClient(edgeBrokerUrl, "Sim_" + idDispositivo);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.connect(options);
            System.out.println("[SimCalciobalilla] Connesso all'Edge: " + edgeBrokerUrl);

            // Generazione dinamica del topic basata sulla gerarchia Locale/Dispositivo
            String topic = MqttCalciobalillaTopics.getTopicPubblicazione(idLocale, idDispositivo);

            while (inEsecuzione) {
                System.out.println("\n[SimCalciobalilla] --- INIZIO NUOVA PARTITA ---");
                PartitaCalciobalilla partita = iniziaNuovaPartita();

                // Ciclo di gioco: prosegue fino al raggiungimento del punteggio massimo
                while (partita.getGoalRossi() < MAX_GOL && partita.getGoalBlu() < MAX_GOL && inEsecuzione) {

                    // Simula il tempo di gioco tra un'azione e l'altra
                    Thread.sleep(2000 + random.nextInt(3000));

                    // Calcolo probabilistico dell'evento (Gol o Fallo)
                    simulaEvento(partita);

                    // Pubblicazione telemetria in tempo reale (Stato corrente)
                    inviaMessaggio(client, topic, partita);
                }

                if (inEsecuzione) {
                    // Chiusura ufficiale del match e marcatura temporale
                    partita.setOrarioFine(LocalDateTime.now());

                    String vincitore = partita.getGoalRossi() == MAX_GOL ? "ROSSI" : "BLU";
                    System.out.println("[SimCalciobalilla] PARTITA TERMINATA! Vittoria " + vincitore);

                    // Invio dell'ultimo pacchetto dati marcato come 'concluso'
                    inviaMessaggio(client, topic, partita);

                    // Periodo di cooldown prima del prossimo match
                    System.out.println("[SimCalciobalilla] Pausa tra i match...");
                    Thread.sleep(10000);
                }
            }

            client.disconnect();

        } catch (MqttException | InterruptedException e) {
            System.err.println("[SimCalciobalilla] Errore critico nel motore di simulazione: " + e.getMessage());
        }
    }

    /**
     * Crea un'istanza vergine di {@link PartitaCalciobalilla}.
     *
     * @return Oggetto partita con punteggi azzerati e orario di inizio corrente.
     */
    private PartitaCalciobalilla iniziaNuovaPartita() {
        PartitaCalciobalilla p = new PartitaCalciobalilla(0, 0, 0, 0, 0);
        p.setOrarioInizio(LocalDateTime.now());
        return p;
    }

    /**
     * Motore stocastico della partita.
     * Applica pesi probabilistici per determinare l'andamento del gioco.
     */
    private void simulaEvento(PartitaCalciobalilla partita) {
        int probabilita = random.nextInt(100);

        if (probabilita < 40) { // 40% probabilità: Gol Squadra Rossa
            partita.setGoalRossi(partita.getGoalRossi() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
            System.out.println("   -> GOAL ROSSI! (" + partita.getGoalRossi() + " - " + partita.getGoalBlu() + ")");
        } else if (probabilita < 80) { // 40% probabilità: Gol Squadra Blu
            partita.setGoalBlu(partita.getGoalBlu() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
            System.out.println("   -> GOAL BLU! (" + partita.getGoalRossi() + " - " + partita.getGoalBlu() + ")");
        } else { // 20% probabilità: Rullata (Fallo tecnico)
            partita.setTotaleRullate(partita.getTotaleRullate() + 1);
            System.out.println("   -> FALLO! (Rullata registrata)");
        }

        // Simula la velocità media della pallina per quel turno
        partita.setDurataMediaPallinaSecondi(10 + random.nextInt(20));
    }

    /**
     * Trasforma l'oggetto in JSON e lo pubblica tramite il client MQTT fornito.
     *
     * @param client  Client MQTT connesso.
     * @param topic   Canale su cui pubblicare.
     * @param partita Oggetto da serializzare.
     * @throws MqttException In caso di fallimento della comunicazione.
     */
    private void inviaMessaggio(MqttClient client, String topic, PartitaCalciobalilla partita) throws MqttException {
        String payloadJson = gson.toJson(partita);
        MqttMessage message = new MqttMessage(payloadJson.getBytes());

        // QoS 0 (At most once): ideale per telemetria frequente ad alta velocità
        message.setQos(0);
        client.publish(topic, message);
    }
}
