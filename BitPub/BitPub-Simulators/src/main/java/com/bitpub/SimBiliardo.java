package com.bitpub;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

/**
 * Classe di dominio per modellare l'evento del Biliardo.
 * GSON trasformerà automaticamente questa classe in JSON.
 */
class EventoBiliardo {
    String tipoEvento;
    String giocatore;
    int pallaImbucata;
    long timestamp;

    public EventoBiliardo(String tipoEvento, String giocatore, int pallaImbucata) {
        this.tipoEvento = tipoEvento;
        this.giocatore = giocatore;
        this.pallaImbucata = pallaImbucata;
        this.timestamp = System.currentTimeMillis();
    }
}

/**
 * Simulatore Multithread per il Biliardo.
 */
public class SimBiliardo implements Runnable {

    private String idLocale;
    private String idTavolo;
    private String brokerUrl; // Es. "tcp://127.0.0.1:1883"
    private boolean inEsecuzione;

    // Nomi di esempio per i giocatori
    private final String[] giocatori = {"Luca", "Stefano", "Timothy"};

    public SimBiliardo(String idLocale, String idTavolo, String ipEdge) {
        this.idLocale = idLocale;
        this.idTavolo = idTavolo;
        // Connessione locale senza TLS
        this.brokerUrl = "tcp://" + ipEdge + ":1883";
        this.inEsecuzione = true;
    }

    public void fermaSimulatore() {
        this.inEsecuzione = false;
    }

    @Override
    public void run() {
        System.out.println("[SimBiliardo] Avvio simulatore per il tavolo " + idTavolo);

        try {
            // Inizializzazione del client MQTT Paho
            MqttClient client = new MqttClient(brokerUrl, "Simulatore_" + idTavolo, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // Il sensore non ha bisogno di sessioni durevoli, se cade riparte da zero

            System.out.println("[SimBiliardo] Connessione all'Edge Node: " + brokerUrl);
            client.connect(options);

            Random random = new Random();
            Gson gson = new Gson(); // Inizializziamo GSON per creare il JSON

            // Topic specifico senza wildcard
            String topicImbucate = "bitpub/locali/" + idLocale + "/biliardo/" + idTavolo + "/imbucate";

            // Ciclo infinito che simula la partita
            while (inEsecuzione) {
                // Simula il tempo di un turno (attesa casuale tra 2 e 6 secondi)
                int attesa = 2000 + random.nextInt(4000);
                Thread.sleep(attesa);

                // Generazione dei dati fittizi
                String giocatoreCorrente = giocatori[random.nextInt(giocatori.length)];
                int palla = 1 + random.nextInt(15); // Palle da 1 a 15

                EventoBiliardo evento = new EventoBiliardo("IMBUCATA", giocatoreCorrente, palla);

                // GSON converte l'oggetto Java in una stringa JSON
                String payloadJson = gson.toJson(evento);

                // Creazione e invio del messaggio MQTT
                MqttMessage message = new MqttMessage(payloadJson.getBytes());
                message.setQos(0); // QoS 0: "At most once" (Fire and forget) per i sensori

                client.publish(topicImbucate, message);
                System.out.println("[SimBiliardo] " + giocatoreCorrente + " ha imbucato la palla " + palla + ". JSON inviato: " + payloadJson);
            }

            client.disconnect();
            System.out.println("[SimBiliardo] Simulatore fermato e disconnesso.");

        } catch (MqttException | InterruptedException e) {
            System.err.println("[SimBiliardo] Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}