package com.bitpub.demo;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Dimostrazione pratica del superamento di NAT/Firewall tramite protocollo MQTT.
 *
 * <p>FLUSSO COMPLETO DIMONSTRATO: 
 * Edge (Rete Privata) --(TCP Outbound)--> NAT/Firewall --(Internet)--> Broker MQTT Pubblico <--(TCP Outbound)-- Cloud/Backend</p>
 *
 * @author Stefano Bellan 20054330
 */
public class NatTraversalDemo {

    public static void main(String[] args) {
        // Utilizziamo un broker pubblico per dimostrare l'uscita dalla rete locale verso Internet
        String brokerUrl = "tcp://broker.hivemq.com:1883";
        String clientId = "Edge-NAT-Demo-" + System.currentTimeMillis();
        String topic = "bitpub/demo/nat/status/" + clientId;

        MemoryPersistence persistence = new MemoryPersistence();

        try {
            System.out.println("=== Avvio Dimostrazione NAT Traversal MQTT ===");
            System.out.println("[INFO] Inizializzazione client Edge locale (dietro NAT/Firewall)");
            
            MqttClient sampleClient = new MqttClient(brokerUrl, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            
            // Impostiamo un timeout e un keep-alive per mantenere il canale aperto attraverso il NAT
            connOpts.setConnectionTimeout(10);
            connOpts.setKeepAliveInterval(60);

            // Callback per gestire i messaggi in ingresso e verificare la bidirezionalità
            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[ERRORE] Connessione persa: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String incomingTopic, MqttMessage message) {
                    // Il messaggio in arrivo dimostra che, sebbene siamo dietro un NAT,
                    // la connessione originariamente aperta in Outbound permette la ricezione di dati (Inbound logico).
                    System.out.println("\n[RICEVUTO DAL BROKER ESTERNO]");
                    System.out.println("Topic: " + incomingTopic);
                    System.out.println("Messaggio: " + new String(message.getPayload()));
                    System.out.println("===============================\n");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Conferma dell'avvenuto invio
                }
            });

            System.out.println("[RETE] Tentativo di connessione OUTBOUND verso: " + brokerUrl);
            
            // Connessione outbound: supera fisicamente il NAT perché è il client a iniziare 
            // la sessione TCP verso l'esterno. Il router "si fida" di questa connessione.
            sampleClient.connect(connOpts);
            System.out.println("[SUCCESSO] Connessione stabilita! Il tunnel attraverso il NAT è aperto.");

            // Sottoscrizione al topic: istruisce il broker a inviare messaggi verso questo client
            System.out.println("[SOTTOSCRIZIONE] In ascolto sul topic: " + topic);
            sampleClient.subscribe(topic);

            // Simuliamo il lavoro dell'Edge Device pubblicando lo stato
            String content = "Edge Device Connesso. Segnale OK. Superamento NAT completato.";
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(1); // Garantisce che il messaggio venga consegnato almeno una volta

            System.out.println("[INVIO] Pubblicazione dati verso il Broker pubblico...");
            sampleClient.publish(topic, message);
            
            System.out.println("[ATTESA] Aspettiamo 5 secondi per ricevere il nostro stesso messaggio dal Broker...");
            // Attendiamo per permettere al messaggio di viaggiare verso il broker pubblico e tornare indietro
            Thread.sleep(5000);

            // Disconnessione pulita
            sampleClient.disconnect();
            System.out.println("[CHIUSURA] Disconnesso dal broker. Tunnel NAT richiuso.");
            
        } catch (MqttException me) {
            System.out.println("[ERRORE MQTT] Motivo: " + me.getReasonCode());
            System.out.println("Messaggio: " + me.getMessage());
            System.out.println("Causa: " + me.getCause());
            me.printStackTrace();
        } catch (InterruptedException ie) {
            System.out.println("[ERRORE THREAD] Attesa interrotta.");
        }
    }
}