package com.bitpub;

import com.bitpub.SimFreccette;
import com.bitpub.SimCalciobalilla;
import com.bitpub.SimBiliardo;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Questa classe orchestra l'inizializzazione e l'avvio concorrente dei simulatori
 * per Freccette, Calciobalilla e Biliardo, collegandoli all'Edge Gateway locale.
 *
 * @author Stefano Bellan 20054330 Timothy Giolito

 */
public class Main {

    private static SimFreccette simFreccette;
    private static SimCalciobalilla simCalciobalilla;
    private static SimBiliardo simBiliardo;
    
    private static Thread threadFreccette;
    private static Thread threadCalciobalilla;
    private static Thread threadBiliardo;

    private static final String ID_LOCALE = "pub_centrale";
    private static final String IP_EDGE_NODO = "127.0.0.1";

    public static void main(String[] args) {
        System.out.println("--- Simulatori IoT BitPub in attesa di comandi ---");
        System.out.println("In attesa di comandi MQTT dal Cloud per avviare i giochi...");

        try {
            MqttClient client = new MqttClient("tcp://" + IP_EDGE_NODO + ":1883", "Simulators-Daemon-Main");
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Connessione persa, il demone potrebbe non ricevere i comandi!");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("[Main Daemon] Ricevuto comando su " + topic + ": " + payload);
                    if (topic.contains("foosball/start")) {
                        startCalciobalilla();
                    } else if (topic.contains("darts/start")) {
                        startFreccette();
                    } else if (topic.contains("billiards/start")) {
                        startBiliardo();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });
            client.connect();
            client.subscribe("bitpub/cloud/+/start");
            System.out.println("Iscritto a bitpub/cloud/+/start !");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void startFreccette() {
        if (threadFreccette == null || !threadFreccette.isAlive()) {
            simFreccette = new SimFreccette(ID_LOCALE, "freccette_A", IP_EDGE_NODO);
            threadFreccette = new Thread(simFreccette);
            threadFreccette.setDaemon(true);
            threadFreccette.start();
            System.out.println("Simulatore Freccette avviato.");
        }
    }

    public static void startCalciobalilla() {
        if (threadCalciobalilla == null || !threadCalciobalilla.isAlive()) {
            simCalciobalilla = new SimCalciobalilla(ID_LOCALE, "calciobalilla_1", IP_EDGE_NODO);
            threadCalciobalilla = new Thread(simCalciobalilla);
            threadCalciobalilla.setDaemon(true);
            threadCalciobalilla.start();
            System.out.println("Simulatore Calciobalilla avviato.");
        }
    }

    public static void startBiliardo() {
        if (threadBiliardo == null || !threadBiliardo.isAlive()) {
            simBiliardo = new SimBiliardo(ID_LOCALE, "biliardo_1", IP_EDGE_NODO);
            threadBiliardo = new Thread(simBiliardo);
            threadBiliardo.setDaemon(true);
            threadBiliardo.start();
            System.out.println("Simulatore Biliardo avviato.");
        }
    }
}
