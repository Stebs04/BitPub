package com.bitpub;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
// IMPORT AGGIUNTI PER LA CODA
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Utilizza uno ScheduledExecutorService per gestire i task senza thread crudi.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        String ipEdgeNodo = "127.0.0.1";

        // 1. CREAZIONE DELLA CODA CONDIVISA (Il nostro nuovo "Event Bus" locale)
        BlockingQueue<Object> codaLocale = new LinkedBlockingQueue<>();

        // 2. Creiamo uno Schedulatore (Pool di thread gestito da Java)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

        // 3. Aggiungiamo l'hook di spegnimento (Graceful Shutdown)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SISTEMA] Spegnimento richiesto. Chiusura sicura in corso...");
            scheduler.shutdownNow(); // Ferma tutti i simulatori in esecuzione
            // Qui in futuro aggiungeremo lo svuotamento della coda e la disconnessione MQTT
            System.out.println("[SISTEMA] Dispositivi spenti. Arrivederci!");
        }));

        try {
            org.eclipse.paho.client.mqttv3.MqttClient client = new org.eclipse.paho.client.mqttv3.MqttClient("tcp://" + ipEdgeNodo + ":1883", "MainSimulatorController");
            client.connect();

            System.out.println("In attesa di comandi di avvio dalla View...");

            client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {

                // Usiamo i Future per poter eventualmente cancellare (stoppare) i task
                private ScheduledFuture<?> taskCalciobalilla;
                private ScheduledFuture<?> taskFreccette;
                private ScheduledFuture<?> taskBiliardo;

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connessione persa al broker MQTT.");
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    String msg = new String(message.getPayload()).toLowerCase();
                    String idLocale = "pub_centrale";

                    if (topic.contains("calciobalilla") || topic.contains("foosball") || msg.contains("calciobalilla") || msg.contains("foosball")) {
                        if (taskCalciobalilla == null || taskCalciobalilla.isDone()) {
                            System.out.println("Avviando simulatore Calciobalilla...");
                            // MODIFICA: Passiamo 'codaLocale' al costruttore
                            SimCalciobalilla sim = new SimCalciobalilla(idLocale, "calciobalilla_1", codaLocale);
                            // Schedula il task per essere eseguito ogni 4 secondi
                            taskCalciobalilla = scheduler.scheduleAtFixedRate(sim, 0, 4, TimeUnit.SECONDS);
                        }
                    } else if (topic.contains("freccette") || msg.toLowerCase().contains("freccette")) {
                        if (taskFreccette == null || taskFreccette.isDone()) {
                            System.out.println("Avviando simulatore Freccette...");
                            // MODIFICA: Passiamo 'codaLocale' al costruttore
                            SimFreccette sim = new SimFreccette(idLocale, "freccette_A", codaLocale);
                            // Schedula il task per essere eseguito ogni 2 secondi
                            taskFreccette = scheduler.scheduleAtFixedRate(sim, 0, 2, TimeUnit.SECONDS);
                        }
                    } else if (topic.contains("biliardo") || msg.toLowerCase().contains("biliardo")) {
                        if (taskBiliardo == null || taskBiliardo.isDone()) {
                            System.out.println("Avviando simulatore Biliardo...");
                            // MODIFICA: Passiamo 'codaLocale' al costruttore
                            SimBiliardo sim = new SimBiliardo(idLocale, "biliardo_1", codaLocale);
                            // Schedula il task per essere eseguito ogni 5 secondi
                            taskBiliardo = scheduler.scheduleAtFixedRate(sim, 0, 5, TimeUnit.SECONDS);
                        }
                    }
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {}
            });

            // Sottoscrizione ai topic di avvio
            client.subscribe("bitpub/cloud/foosball/start");
            client.subscribe("bitpub/cloud/calciobalilla/start");
            client.subscribe("bitpub/cloud/freccette/start");
            client.subscribe("bitpub/cloud/biliardo/start");
            client.subscribe("bitpub/simulators/start");

        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            System.err.println("Errore di connessione MQTT iniziale: " + e.getMessage());
        }
    }
}