/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Configurazione infrastrutturale per l'integrazione MQTT del servizio catalogo giochi.
 * 
 * Canale Outbound: distribuisce gli snapshot di configurazione in modalità 'retained',
 * assicurando che qualsiasi simulatore connesso in ritardo riceva i parametri operativi correnti.
 * 
 * Canale Inbound: implementa una sottoscrizione persistente per i comandi di gestione catalogo
 * instradati dal nodo Edge. Evita chiamate REST dirette adottando lo stesso paradigma asincrono
 * degli altri microservizi core.
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.inbound-client-id}")
    private String inboundClientId;

    @Value("${mqtt.topic.system-action}")
    private String systemActionTopic;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultTopic("bitpub/config/games/default");
        handler.setDefaultRetained(true); // Consente ai client che si collegano successivamente di ricevere l'ultima configurazione nota
        return handler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface ConfigPublisher {
        void publish(String payload, @Header(MqttHeaders.TOPIC) String topic);
    }

    // ── Gestione Inbound: sottoscrizione ai comandi inoltrati dall'Edge ───────────
    // La sessione persistente (cleanSession=false) abbinata a un clientId statico garantisce
    // la conservazione dei messaggi QoS1 in caso di indisponibilità momentanea del servizio.

    @Bean
    public MqttPahoClientFactory mqttInboundClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel systemActionInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer systemActionInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(inboundClientId, mqttInboundClientFactory(), systemActionTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(systemActionInboundChannel());
        return adapter;
    }
}
