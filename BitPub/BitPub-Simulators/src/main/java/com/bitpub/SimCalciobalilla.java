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
 * @implNote Modifiche e integrazioni apportate da Stefano Bellan 20054330.
 */
public class SimCalciobalilla implements Runnable {

    private final String idLocale;
    private final String idDispositivo;
    private final String edgeBrokerUrl;
    private final Gson gson;
    private final Random random;

    private final int MAX_GOL = 10;
    private volatile boolean inEsecuzione = true;

    /**
     * Inizializza il simulatore configurando i parametri di connessione MQTT e i parser JSON.
     *
     * @param idLocale      Identificativo univoco del locale o punto vendita.
     * @param idDispositivo Identificativo univoco del tavolo fisico da calciobalilla.
     * @param edgeBrokerIp  Indirizzo IP del broker MQTT (Edge Gateway locale).
     */
    public SimCalciobalilla(String idLocale, String idDispositivo, String edgeBrokerIp) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        this.edgeBrokerUrl = "tcp://" + edgeBrokerIp + ":1883";

        // Configurazione per esporre in formato JSON solo i campi contrassegnati con @Expose
        this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        this.random = new Random();
    }

    /**
     * Arresta in modo sicuro il loop infinito della simulazione.
     * Permette la chiusura dolce del thread di background.
     */
    public void fermaSimulatore() {
        this.inEsecuzione = false;
    }

    /**
     * Punto di ingresso del thread. Gestisce la connessione al broker MQTT
     * e genera continuamente nuove partite simulate fino all'arresto.
     */
    @Override
    public void run() {
        try {
            MqttClient client = new MqttClient(edgeBrokerUrl, "Sim_" + idDispositivo);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.connect(options);
            System.out.println("[SimCalciobalilla] Connesso all'Edge: " + edgeBrokerUrl);

            String topic = MqttCalciobalillaTopics.getTopicPubblicazione(idLocale, idDispositivo);

            // Avvio del ciclo continuo di partite
            while (inEsecuzione) {
                System.out.println("\n[SimCalciobalilla] --- INIZIO NUOVA PARTITA ---");
                PartitaCalciobalilla partita = iniziaNuovaPartita();

                // Loop di gioco: prosegue fino alla vittoria di una delle due squadre
                while (partita.getGoalRossi() < MAX_GOL && partita.getGoalBlu() < MAX_GOL && inEsecuzione) {

                    Thread.sleep(2000 + random.nextInt(3000));
                    simulaEvento(partita);
                    inviaMessaggio(client, topic, partita);
                }

                if (inEsecuzione) {
                    partita.setOrarioFine(LocalDateTime.now());
                    String vincitore = partita.getGoalRossi() == MAX_GOL ? "ROSSI" : "BLU";
                    System.out.println("[SimCalciobalilla] PARTITA TERMINATA! Vittoria " + vincitore);

                    inviaMessaggio(client, topic, partita);

                    // Tempo di riposo del tavolo tra un match e il successivo
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
     * Inizializza un nuovo oggetto Partita azzerando i punteggi e settando l'orario di inizio.
     *
     * @return L'oggetto PartitaCalciobalilla appena istanziato e pronto.
     */
    private PartitaCalciobalilla iniziaNuovaPartita() {
        PartitaCalciobalilla p = new PartitaCalciobalilla(0, 0, 0, 0, 0);
        p.setOrarioInizio(LocalDateTime.now());
        return p;
    }

    /**
     * Genera un evento casuale durante il gioco (Gol Rosso, Gol Blu o Fallo)
     * e aggiorna di conseguenza le statistiche della partita corrente.
     *
     * @param partita La partita in corso da aggiornare.
     */
    private void simulaEvento(PartitaCalciobalilla partita) {
        int probabilita = random.nextInt(100);

        // Distribuzione probabilistica degli eventi sul tavolo
        if (probabilita < 40) {
            partita.setGoalRossi(partita.getGoalRossi() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
            System.out.println("   -> GOAL ROSSI! (" + partita.getGoalRossi() + " - " + partita.getGoalBlu() + ")");
        } else if (probabilita < 80) {
            partita.setGoalBlu(partita.getGoalBlu() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
            System.out.println("   -> GOAL BLU! (" + partita.getGoalRossi() + " - " + partita.getGoalBlu() + ")");
        } else {
            partita.setTotaleRullate(partita.getTotaleRullate() + 1);
            System.out.println("   -> FALLO! (Rullata registrata)");
        }

        partita.setDurataMediaPallinaSecondi(10 + random.nextInt(20));
    }

    /**
     * Converte i dati della partita in JSON e li pubblica sul broker MQTT.
     *
     * @param client  Il client MQTT regolarmente connesso.
     * @param topic   Il canale MQTT di destinazione.
     * @param partita L'oggetto contenente lo stato del gioco da trasmettere.
     * @throws MqttException Se la comunicazione via rete fallisce.
     */
    private void inviaMessaggio(MqttClient client, String topic, PartitaCalciobalilla partita) throws MqttException {
        String payloadJson = gson.toJson(partita);
        MqttMessage message = new MqttMessage(payloadJson.getBytes());
        message.setQos(0);
        client.publish(topic, message);
    }
}