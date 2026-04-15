package com.bitpub;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.utils.MqqtCalciobalillaTopics;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;

/**
 * Simulatore software di un tavolo da calciobalilla smart.
 * <p>
 * Questa classe implementa {@link Runnable} per gestire in un thread separato la connessione
 * MQTT verso un broker locale (Edge) e l'invio periodico di telemetria e stati di gioco.
 * </p>
 *
 * @author Stefano Bellan 20054330
 */
public class SimCalciobalilla implements Runnable {

    private final String idLocale;
    private final String idDispositivo;
    private final String edgeBrokerUrl;
    private final Gson gson;
    private final Random random;

    /**
     * Inizializza una nuova istanza del simulatore.
     *
     * @param idLocale      Identificativo del locale di installazione.
     * @param idDispositivo Identificativo univoco del tavolo.
     * @param edgeBrokerIp  Indirizzo IP o hostname del broker MQTT locale.
     */
    public SimCalciobalilla(String idLocale, String idDispositivo, String edgeBrokerIp) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        // Protocollo TCP standard sulla porta 1883 (default MQTT senza SSL)
        this.edgeBrokerUrl = "tcp://" + edgeBrokerIp + ":1883";
        this.gson = new Gson();
        this.random = new Random();
    }

    /**
     * Loop principale del simulatore.
     * Gestisce la connessione al broker, la serializzazione JSON e l'invio dei dati
     * con intervalli di tempo randomici.
     */
    @Override
    public void run() {
        try {
            // Client MQTT identificato dal prefisso 'Sim_' per facilitare il debug lato broker
            MqttClient client = new MqttClient(edgeBrokerUrl, "Sim_" + idDispositivo);
            MqttConnectOptions options = new MqttConnectOptions();

            // Forza una sessione pulita per evitare il recupero di messaggi arretrati
            options.setCleanSession(true);

            client.connect(options);
            System.out.println("Calciobalilla " + idDispositivo + " connesso all'Edge: " + edgeBrokerUrl);

            // Recupera il topic corretto dalla utility class dedicata
            String topic = MqqtCalciobalillaTopics.getTopicPubblicazione(idLocale, idDispositivo);

            // Stato interno della partita corrente
            PartitaCalciobalilla partita = new PartitaCalciobalilla();

            while (true) {
                // Genera una variazione casuale dello stato di gioco
                simulaEvento(partita);

                // Serializzazione dell'oggetto POJO in formato JSON
                String payloadJson = gson.toJson(partita);

                // Configurazione messaggio: QoS 0 (At most once) per ridurre l'overhead
                MqttMessage message = new MqttMessage(payloadJson.getBytes());
                message.setQos(0);

                client.publish(topic, message);
                System.out.println("Pubblicato su " + topic + ": " + payloadJson);

                // Delay variabile tra 3 e 8 secondi per simulare un comportamento umano/fisico
                Thread.sleep(3000 + random.nextInt(5000));
            }
        } catch (MqttException | InterruptedException e) {
            // Logging semplificato dell'errore (in produzione utilizzare un logger come Log4j)
            System.err.println("Errore nel simulatore Calciobalilla: " + e.getMessage());
        }
    }

    /**
     * Applica logica stocastica per aggiornare i contatori della partita.
     * <p>
     * Probabilità definite:
     * <ul>
     *     <li>40% -> Goal Squadra Rossa</li>
     *     <li>40% -> Goal Squadra Blu</li>
     *     <li>15% -> Rullata (infrazione)</li>
     *     <li>5%  -> Aggiornamento durata media azione</li>
     * </ul>
     * </p>
     *
     * @param partita L'oggetto stato da modificare.
     */
    private void simulaEvento(PartitaCalciobalilla partita) {
        int probabilita = random.nextInt(100);

        if (probabilita < 40) {
            partita.setGoalRossi(partita.getGoalRossi() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
        } else if (probabilita < 80) {
            partita.setGoalBlu(partita.getGoalBlu() + 1);
            partita.setTotaleGol(partita.getTotaleGol() + 1);
        } else if (probabilita < 95) {
            partita.setTotaleRullate(partita.getTotaleRullate() + 1);
        } else {
            // Simuliamo una variazione della velocità di gioco (10-40 secondi)
            int nuovaDurata = 10 + random.nextInt(31);
            partita.setDurataMediaPallinaSecondi(nuovaDurata);
        }
    }
}
