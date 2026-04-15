package com.bitpub.simulators;

import com.bitpub.utils.MqttFreccetteTopics;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

/**
 * Thread Simulatore per un bersaglio di Freccette.
 */
public class SimFreccette implements Runnable {

    private String idLocale;
    private String idDispositivo;
    private String brokerIp;
    private int punteggio;
    private Random random;

    // Costruttore: prepariamo il nostro giocatore virtuale
    public SimFreccette(String idLocale, String idDispositivo, String brokerIp) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        this.brokerIp = brokerIp;
        this.punteggio = 501;
        this.random = new Random();
    }

    @Override
    public void run() {
        // L'indirizzo "tcp://" indica una connessione non criptata (Senza TLS)
        String brokerUrl = "tcp://" + brokerIp + ":1883";
        MqttClient mqttClient = null;

        try {
            // 1. Connessione al broker Edge tramite Eclipse Paho
            mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("SimFreccette [" + idDispositivo + "] in connessione a " + brokerUrl);
            mqttClient.connect(connOpts);
            System.out.println("SimFreccette [" + idDispositivo + "] Connesso!");

            // Invia evento di inizio partita
            pubblicaEvento(mqttClient, "Inizio Partita (501)");

            // 2. Ciclo di gioco principale
            while (punteggio > 0) {
                // Aspettiamo 2 secondi per simulare il tempo del lancio
                Thread.sleep(2000);

                // Generiamo un punteggio realistico per un lancio (es. da 0 a 60, il massimo con un triplo 20)
                int puntiLancio = random.nextInt(61);
                System.out.println("[" + idDispositivo + "] Freccetta lanciata! Punti: " + puntiLancio);

                // 3. Logica del gioco: Busto, Bullseye o Lancio Normale
                if (puntiLancio == 50) {
                    pubblicaEvento(mqttClient, "Bullseye!");
                    punteggio -= puntiLancio;
                }
                else if (punteggio - puntiLancio < 0 || punteggio - puntiLancio == 1) {
                    // Se va sotto zero o rimane a 1 (non posso chiudere con un doppio)
                    pubblicaEvento(mqttClient, "Busto! Lancio annullato.");
                    // Il punteggio ritorna a quello di inizio turno.
                }
                else {
                    // Lancio normale
                    punteggio -= puntiLancio;
                }

                // 4. Pubblica il punteggio aggiornato
                pubblicaScore(mqttClient);
            }

            // Fine partita
            pubblicaEvento(mqttClient, "Partita Terminata! Vittoria.");
            System.out.println("SimFreccette [" + idDispositivo + "] Partita Conclusa.");

            // Disconnessione pulita
            mqttClient.disconnect();

        } catch (MqttException | InterruptedException e) {
            System.out.println("Errore nel simulatore freccette: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- METODI DI SUPPORTO PER LA PUBBLICAZIONE MQTT ---

    private void pubblicaScore(MqttClient client) throws MqttException {
        // Recuperiamo il topic usando la TUA classe Utility!
        String topic = MqttFreccetteTopics.getScoreTopic(idLocale, idDispositivo);

        // Creiamo un semplice JSON manuale (potrai sostituirlo con il tuo JsonManager)
        String payload = "{ \"punteggioRimasto\": " + punteggio + " }";

        inviaMessaggioMqtt(client, topic, payload);
    }

    private void pubblicaEvento(MqttClient client, String tipoEvento) throws MqttException {
        String topic = MqttFreccetteTopics.getEventiTopic(idLocale, idDispositivo);
        String payload = "{ \"evento\": \"" + tipoEvento + "\" }";

        inviaMessaggioMqtt(client, topic, payload);
    }

    private void inviaMessaggioMqtt(MqttClient client, String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1); // QoS 1: Assicura che il messaggio venga consegnato almeno una volta
        client.publish(topic, message);
    }
}