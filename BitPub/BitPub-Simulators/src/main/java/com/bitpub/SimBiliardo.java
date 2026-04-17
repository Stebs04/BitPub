package it.bitpub.simulators.biliardo;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class BiliardoSimulator implements Runnable {
    private String idLocale;
    private String idTavolo;
    private MqttClient mqttClient;

    public BiliardoSimulator(String idLocale, String idTavolo, MqttClient client) {
        this.idLocale = idLocale;
        this.idTavolo = idTavolo;
        this.mqttClient = client;
    }

    @Override
    public void run() {
        try {
            // Costruiamo il topic reale sostituendo le wildcard (+) con gli ID veri
            String topicImbucata = "bitpub/locali/" + idLocale + "/biliardo/" + idTavolo + "/imbucate";

            // Creiamo un payload fittizio (in futuro userai GSON per serializzare un oggetto reale)
            String payloadJson = "{\"giocatore\":\"Luca\", \"palla\": 8, \"tipo\":\"imbucata\"}";

            MqttMessage message = new MqttMessage(payloadJson.getBytes());
            message.setQos(1); // Garantisce la consegna

            mqttClient.publish(topicImbucata, message);
            System.out.println("[Biliardo " + idTavolo + "] Evento pubblicato su: " + topicImbucata);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}