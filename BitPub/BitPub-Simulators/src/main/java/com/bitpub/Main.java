package com.bitpub;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.utils.JsonManager;
import com.bitpub.utils.MqttCalciobalillaTopics;
import com.bitpub.utils.MqttFreccetteTopics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Utilizza uno ScheduledExecutorService per gestire i task senza thread crudi.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        String ipEdgeNodo = "127.0.0.1";

        // 1. CREAZIONE DELLA CODA CONDIVISA (Il nostro "Event Bus" locale)
        BlockingQueue<Object> codaLocale = new LinkedBlockingQueue<>();

        // 2. Creiamo uno Schedulatore (Pool di thread gestito da Java)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

        // 3. Aggiungiamo l'hook di spegnimento (Graceful Shutdown)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SISTEMA] Spegnimento richiesto. Chiusura sicura in corso...");
            scheduler.shutdownNow(); // Ferma tutti i simulatori in esecuzione
            System.out.println("[SISTEMA] Dispositivi spenti. Arrivederci!");
        }));

        try {
            // Creiamo e connettiamo il client MQTT
            MqttClient client = new MqttClient("tcp://" + ipEdgeNodo + ":1883", "MainSimulatorController");
            client.connect();

            System.out.println("In attesa di comandi di avvio dalla View...");

            // --- NUOVO COMPONENTE: IL THREAD PUBBLICATORE ---
            // Questo thread prende i dati dalla coda locale e li invia fisicamente all'Edge Node
            new Thread(() -> {
                System.out.println("[SISTEMA] Thread di pubblicazione MQTT avviato.");
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Preleva il prossimo evento disponibile (si blocca se la coda è vuota, senza consumare CPU)
                        Object evento = codaLocale.take();
                        
                        // Convertiamo l'oggetto Java in una stringa in formato JSON
                        String rawJson = JsonManager.getInstance().toJson(evento);
                        
                        // Aggiungiamo le chiavi di sicurezza richieste dallo Strict Filter dell'Edge (Anti-Spoofing)
                        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();
                        jsonObject.addProperty("source", "DEVICE");
                        jsonObject.addProperty("hardwareSignature", "sim-signature-1234");
                        
                        String finalPayload = jsonObject.toString();
                        String topic = "";
                        
                        // Scopriamo di che tipo di evento si tratta e scegliamo il topic MQTT (canale) corretto
                        if (evento instanceof PartitaCalciobalilla) {
                            topic = MqttCalciobalillaTopics.getTopicPubblicazione("pub_centrale", "calciobalilla_1");
                        } else if (evento instanceof EventoFreccette) {
                            topic = MqttFreccetteTopics.getScoreTopic("pub_centrale", "freccette_A");
                        } else if (evento instanceof EventoBiliardo) {
                            topic = "bitpub/locali/pub_centrale/biliardo/biliardo_1/imbucate";
                        } else {
                            System.err.println("[Publisher] Tipo di evento sconosciuto, salto la pubblicazione.");
                            continue; // Passa all'elemento successivo
                        }

                        // Se siamo connessi, finalmente pubblichiamo l'evento all'Edge Node!
                        if (client.isConnected()) {
                            MqttMessage message = new MqttMessage(finalPayload.getBytes(StandardCharsets.UTF_8));
                            message.setQos(0); // Quality of Service 0: per i simulatori va bene un invio rapido
                            client.publish(topic, message);
                            System.out.println("[Publisher] Dato inviato all'Edge sul topic: " + topic);
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("[SISTEMA] Thread di pubblicazione interrotto.");
                    Thread.currentThread().interrupt(); // Chiudiamo pulitamente
                } catch (Exception e) {
                    System.err.println("[SISTEMA] Errore imprevisto nel thread publisher: " + e.getMessage());
                }
            }).start();
            // --- FINE NUOVO COMPONENTE ---

            // Gestione dei comandi in arrivo (start dei simulatori)
            client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {

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
                            SimCalciobalilla sim = new SimCalciobalilla(idLocale, "calciobalilla_1", codaLocale);
                            taskCalciobalilla = scheduler.scheduleAtFixedRate(sim, 0, 4, TimeUnit.SECONDS);
                        }
                    } else if (topic.contains("freccette") || msg.toLowerCase().contains("freccette")) {
                        if (taskFreccette == null || taskFreccette.isDone()) {
                            System.out.println("Avviando simulatore Freccette...");
                            SimFreccette sim = new SimFreccette(idLocale, "freccette_A", codaLocale);
                            taskFreccette = scheduler.scheduleAtFixedRate(sim, 0, 2, TimeUnit.SECONDS);
                        }
                    } else if (topic.contains("biliardo") || msg.toLowerCase().contains("biliardo")) {
                        if (taskBiliardo == null || taskBiliardo.isDone()) {
                            System.out.println("Avviando simulatore Biliardo...");
                            SimBiliardo sim = new SimBiliardo(idLocale, "biliardo_1", codaLocale);
                            taskBiliardo = scheduler.scheduleAtFixedRate(sim, 0, 5, TimeUnit.SECONDS);
                        }
                    }
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {}
            });

            // Sottoscrizione ai topic di avvio provenienti dal cloud/view
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