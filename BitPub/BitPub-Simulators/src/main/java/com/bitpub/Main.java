package com.bitpub;

import com.bitpub.SimFreccette;
import com.bitpub.SimCalciobalilla;
import com.bitpub.SimBiliardo;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Questa classe orchestra l'inizializzazione e l'avvio concorrente dei simulatori
 * per Freccette, Calciobalilla e Biliardo, collegandoli all'Edge Gateway locale.
 *
 * @author Stefano Bellan 20054330 Timothy Giolito

 */
public class Main {

    /**
     * Metodo di avvio del sistema. Configura i parametri di rete e lancia i thread
     * dedicati per ogni dispositivo simulato.
     *
     * @param args Argomenti da riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        String ipEdgeNodo = "127.0.0.1";
        
        try {
            org.eclipse.paho.client.mqttv3.MqttClient client = new org.eclipse.paho.client.mqttv3.MqttClient("tcp://" + ipEdgeNodo + ":1883", "MainSimulatorController");
            client.connect();
            
            System.out.println("In attesa di comandi di avvio dalla View...");
            
            client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {
                private Thread threadCalciobalilla;
                private Thread threadFreccette;
                private Thread threadBiliardo;

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connessione persa al broker MQTT.");
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    String msg = new String(message.getPayload());
                    String idLocale = "pub_centrale";
                    
                    if (topic.contains("calciobalilla") || msg.toLowerCase().contains("calciobalilla")) {
                        if (threadCalciobalilla == null || !threadCalciobalilla.isAlive()) {
                            System.out.println("Avviando simulatore Calciobalilla...");
                            SimCalciobalilla sim = new SimCalciobalilla(idLocale, "calciobalilla_1", ipEdgeNodo);
                            threadCalciobalilla = new Thread(sim);
                            threadCalciobalilla.start();
                        }
                    } else if (topic.contains("freccette") || msg.toLowerCase().contains("freccette")) {
                        if (threadFreccette == null || !threadFreccette.isAlive()) {
                            System.out.println("Avviando simulatore Freccette...");
                            SimFreccette sim = new SimFreccette(idLocale, "freccette_A", ipEdgeNodo);
                            threadFreccette = new Thread(sim);
                            threadFreccette.start();
                        }
                    } else if (topic.contains("biliardo") || msg.toLowerCase().contains("biliardo")) {
                        if (threadBiliardo == null || !threadBiliardo.isAlive()) {
                            System.out.println("Avviando simulatore Biliardo...");
                            SimBiliardo sim = new SimBiliardo(idLocale, "biliardo_1", ipEdgeNodo);
                            threadBiliardo = new Thread(sim);
                            threadBiliardo.start();
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
            e.printStackTrace();
        }
    }
}
