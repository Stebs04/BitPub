package com.bitpub;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bitpub.models.PartitaCalciobalilla;
// Assicurati che i tuoi import per EventoFreccette e EventoBiliardo siano presenti qui sotto
import com.bitpub.utils.JsonManager;
import com.bitpub.utils.MqttCalciobalillaTopics;
import com.bitpub.utils.MqttFreccetteTopics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Gestisce la ricezione dei comandi dal Cloud e pubblica i dati generati.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        String ipEdgeNodo = "127.0.0.1";

        // 1. CREAZIONE DELLA CODA CONDIVISA (Event Bus locale)
        BlockingQueue<Object> codaLocale = new LinkedBlockingQueue<>();

        // 2. Creiamo uno Schedulatore
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        
        // Mappa per tracciare i task attivi per ogni singolo tavolo.
        // Architetturalmente essenziale per permettere di avviare/fermare tavoli multipli in modo indipendente.
        Map<String, ScheduledFuture<?>> taskAttivi = new ConcurrentHashMap<>();

        // 3. Graceful Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SISTEMA] Spegnimento richiesto. Chiusura sicura in corso...");
            scheduler.shutdownNow();
            System.out.println("[SISTEMA] Dispositivi spenti. Arrivederci!");
        }));

        try {
            // Utilizziamo MemoryPersistence per evitare problemi di file locking (molto comuni
            // durante il riavvio forzato dei client in fase di test locale).
            MqttClient client = new MqttClient("tcp://" + ipEdgeNodo + ":1883", "MainSimulatorController", new MemoryPersistence());
            
            // Configuriamo le opzioni di connessione per la massima resilienza
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            
            client.connect(options);
            System.out.println("[SISTEMA] Connesso al broker MQTT su " + ipEdgeNodo);

            // --- THREAD PUBBLICATORE ---
            new Thread(() -> {
                System.out.println("[SISTEMA] Thread di pubblicazione MQTT avviato.");
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Object evento = codaLocale.take();
                        String rawJson = JsonManager.getInstance().toJson(evento);
                        
                        JsonObject jsonObject = JsonParser.parseString(rawJson).getAsJsonObject();
                        jsonObject.addProperty("source", "DEVICE");
                        jsonObject.addProperty("hardwareSignature", "sim-signature-1234");
                        
                        String finalPayload = jsonObject.toString();
                        String topic = "";
                        
                        // L'istanza dell'oggetto definisce il canale di routing corretto
                        if (evento instanceof PartitaCalciobalilla) {
                            topic = MqttCalciobalillaTopics.getTopicPubblicazione("pub_centrale", "calciobalilla_1");
                        } else if (evento.getClass().getSimpleName().equals("EventoFreccette")) {
                            topic = MqttFreccetteTopics.getScoreTopic("pub_centrale", "freccette_A");
                        } else if (evento.getClass().getSimpleName().equals("EventoBiliardo")) {
                            topic = "bitpub/locali/pub_centrale/biliardo/biliardo_1/imbucate";
                        } else {
                            System.err.println("[Publisher] Tipo di evento sconosciuto, salto la pubblicazione.");
                            continue;
                        }

                        if (client.isConnected()) {
                            MqttMessage message = new MqttMessage(finalPayload.getBytes(StandardCharsets.UTF_8));
                            message.setQos(0); 
                            client.publish(topic, message);
                            System.out.println("[Publisher] Dato inviato all'Edge sul topic: " + topic);
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("[SISTEMA] Thread di pubblicazione interrotto.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("[SISTEMA] Errore imprevisto nel thread publisher: " + e.getMessage());
                }
            }).start();

            // --- GESTIONE DEI COMANDI IN INGRESSO ---
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("[SISTEMA] Connessione persa al broker MQTT. " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payloadString = new String(message.getPayload(), StandardCharsets.UTF_8);
                        
                        // LOG FONDAMENTALE PER IL DEBUG: ti mostra se il segnale arriva davvero!
                        System.out.println("\n[MQTT RX] Ricevuto comando sul topic: " + topic);
                        System.out.println("[MQTT RX] Payload: " + payloadString);

                        String idLocale = "pub_centrale";
                        
                        // Parsiamo il JSON per ottenere dinamicamente il tableId inviato dal Cloud
                        JsonObject jsonPayload = null;
                        try {
                            jsonPayload = JsonParser.parseString(payloadString).getAsJsonObject();
                        } catch (Exception e) {
                            System.err.println("[MQTT RX] Attenzione: Payload non in formato JSON standard.");
                        }

                        // Gestione Comandi di START
                        if (topic.contains("start")) {
                            if (topic.contains("calciobalilla") || topic.contains("foosball")) {
                                // Ricaviamo il tableId inviato dal backend, altrimenti usiamo un fallback di sicurezza
                                String tableId = (jsonPayload != null && jsonPayload.has("tableId")) 
                                        ? "calciobalilla_" + jsonPayload.get("tableId").getAsString() 
                                        : "calciobalilla_1";

                                // Controlliamo che questo specifico tavolo non sia già in esecuzione
                                if (!taskAttivi.containsKey(tableId) || taskAttivi.get(tableId).isDone()) {
                                    System.out.println("[SIMULATORE] Avviando il motore di gioco per: " + tableId);
                                    
                                    // Utilizziamo la tua classe SimCalciobalilla senza modificarla!
                                    SimCalciobalilla sim = new SimCalciobalilla(idLocale, tableId, codaLocale);
                                    ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(sim, 0, 4, TimeUnit.SECONDS);
                                    taskAttivi.put(tableId, task);
                                } else {
                                    System.out.println("[SIMULATORE] Il tavolo " + tableId + " è già in funzione.");
                                }
                            }
                            // Gli altri giochi seguiranno la stessa logica...
                        }
                        
                        // Gestione Comandi di STOP FORZATO
                        if (topic.contains("stop")) {
                             if (topic.contains("calciobalilla") || topic.contains("foosball")) {
                                String tableId = (jsonPayload != null && jsonPayload.has("tableId")) 
                                        ? "calciobalilla_" + jsonPayload.get("tableId").getAsString() 
                                        : "calciobalilla_1";
                                        
                                ScheduledFuture<?> task = taskAttivi.get(tableId);
                                if (task != null && !task.isDone()) {
                                    System.out.println("[SIMULATORE] Ricevuto comando di spegnimento forzato per: " + tableId);
                                    task.cancel(true);
                                    taskAttivi.remove(tableId);
                                }
                            }
                        }

                    } catch (Exception e) {
                        // Isoliamo gli errori di logica per impedire che Paho MQTT disconnetta il client.
                        System.err.println("[MQTT RX] Errore imprevisto durante l'elaborazione del messaggio.");
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            // Sottoscrizioni sia per i topic di START che di STOP, QoS 1 per maggiore affidabilità
            client.subscribe("bitpub/cloud/foosball/start", 1);
            client.subscribe("bitpub/cloud/foosball/stop", 1);
            client.subscribe("bitpub/cloud/calciobalilla/start", 1);
            client.subscribe("bitpub/cloud/freccette/start", 1);
            client.subscribe("bitpub/cloud/biliardo/start", 1);

            System.out.println("[SISTEMA] Sottoscrizioni MQTT attive. In attesa di comandi...");

        } catch (MqttException e) {
            System.err.println("[SISTEMA] Errore critico di connessione MQTT iniziale: " + e.getMessage());
            e.printStackTrace();
        }
    }
}