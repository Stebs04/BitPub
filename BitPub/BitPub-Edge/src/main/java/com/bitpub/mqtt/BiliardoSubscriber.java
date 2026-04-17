package it.bitpub.edge.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class EdgeSubscriber {
    private MqttClient mqttClient;

    public EdgeSubscriber(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void iscrivitiTopicBiliardo() {
        try {
            // Usiamo la costante definita al passo 1 (bitpub/locali/+/biliardo/+/imbucate)
            mqttClient.subscribe(BiliardoTopicConstants.TOPIC_IMBUCATE, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    System.out.println("[EDGE] Ricevuta imbucata dal topic " + topic + ": " + payload);

                    // TODO: Inserire il messaggio in una coda protetta da blocco 'synchronized'
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}