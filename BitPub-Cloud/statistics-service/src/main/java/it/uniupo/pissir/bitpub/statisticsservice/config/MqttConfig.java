/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Configurazione infrastrutturale di MQTT per il microservizio statistiche.
 * Stabilisce sia un canale di pubblicazione per trasmettere in tempo reale gli aggiornamenti della classifica,
 * sia una sottoscrizione duratura per ricevere in modo affidabile l'esito delle partite dal match-service.
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.inbound-client-id}")
    private String inboundClientId;

    @Value("${mqtt.topic.cloud-matches-result}")
    private String cloudMatchResultTopic;

    // ── Configurazione Outbound: trasmissione in tempo reale degli aggiornamenti della classifica verso la WebApp ──

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
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
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                clientId + "_out-" + java.util.UUID.randomUUID(), mqttClientFactory());
        handler.setAsync(true);
        // L'assegnazione del topic avviene dinamicamente a livello di singolo messaggio tramite MqttHeaders.TOPIC
        return handler;
    }

    // ── Configurazione Inbound: sottoscrizione garantita e persistente per i risultati del match-service (QoS 1, cleanSession=false) ──
    // L'utilizzo di un Client ID fisso abbinato a una sessione mantenuta sul broker assicura l'accodamento 
    // dei messaggi in caso di indisponibilità del servizio, con successiva riconsegna alla riconnessione.
    // La logica di registrazione è idempotente, scongiurando il rischio di doppi conteggi per le statistiche.

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
    public MessageChannel mqttResultInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttResultInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(inboundClientId, mqttInboundClientFactory(), cloudMatchResultTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttResultInboundChannel());
        return adapter;
    }
}
