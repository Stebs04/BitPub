// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.config;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Configurazione per la comunicazione MQTT tra il microservizio e il broker.
 * Definisce i canali di ingresso e uscita per la gestione degli eventi di gioco,
 * risultati e azioni interattive.
 */
@Configuration
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic.cloud-sensors}")
    private String cloudSensorTopic;

    @Value("${mqtt.inbound-client-id}")
    private String inboundClientId;

    @Value("${mqtt.topic.cloud-commands}")
    private String cloudCommandTopic;

    @Value("${mqtt.command-inbound-client-id}")
    private String commandInboundClientId;

    @Value("${mqtt.topic.cloud-match-results}")
    private String cloudMatchResultTopic;

    @Value("${mqtt.match-result-inbound-client-id}")
    private String matchResultInboundClientId;

    // ── Uscita: pubblicazione degli aggiornamenti di stato verso la WebApp ─────────

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
        org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler messageHandler =
                new org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler(clientId + "_out-" + java.util.UUID.randomUUID().toString(), mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("bitpub/unrouted/match/state");
        return messageHandler;
    }

    // ── Ingresso: iscrizione agli eventi dei sensori inoltrati dall'Edge ─────────
    // L'utilizzo di una sessione duratura (cleanSession=false e clientId fisso) assicura 
    // l'accodamento degli eventi da parte del broker in caso di indisponibilità del servizio,
    // garantendo l'elaborazione degli eventi al ripristino della connessione.

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
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(inboundClientId, mqttInboundClientFactory(), cloudSensorTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    // ── Ingresso: iscrizione alle azioni interattive inoltrate dall'Edge ─────────
    // Subscriber isolato per una gestione indipendente dei payload di comando, preservando 
    // le garanzie fornite dalla coda durevole (QoS 1).

    @Bean
    public MessageChannel mqttCommandInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttCommandInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(commandInboundClientId, mqttInboundClientFactory(), cloudCommandTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttCommandInboundChannel());
        return adapter;
    }

    // ── Ingresso: iscrizione ai risultati finali inoltrati dall'Edge ─────────────
    // Il nodo Edge invia i dati aggregati di fine partita tramite questo topic in alternativa
    // alla precedente sincronizzazione via REST. L'idempotenza del gestore consente riconsegne sicure.

    @Bean
    public MessageChannel mqttMatchResultInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttMatchResultInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(matchResultInboundClientId, mqttInboundClientFactory(), cloudMatchResultTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttMatchResultInboundChannel());
        return adapter;
    }
}
