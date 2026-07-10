/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.config;

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
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Configurazione per la comunicazione MQTT del servizio tornei.
 * Gestisce sia la pubblicazione degli aggiornamenti verso la WebApp
 * sia la ricezione dei risultati e dei comandi di sistema.
 */
@Configuration
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.inbound-client-id}")
    private String inboundClientId;

    @Value("${mqtt.topic.cloud-results}")
    private String cloudResultTopic;

    @Value("${mqtt.match-result-client-id}")
    private String matchResultClientId;

    @Value("${mqtt.system-action-client-id}")
    private String systemActionClientId;

    @Value("${mqtt.topic.system-action}")
    private String systemActionTopic;

    // Canale in uscita: pubblica gli aggiornamenti di stato del torneo in tempo reale verso la WebApp

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
        handler.setDefaultTopic("bitpub/unrouted/tournament/state");
        return handler;
    }

    // Canale in entrata: sottoscrizione durevole per i risultati del torneo inoltrati dall'Edge.
    // Utilizziamo QoS1 e sessioni non pulite per garantire che il broker accodi i messaggi se il servizio va giù,
    // ritentando la consegna alla riconnessione. Dato che gli aggiornamenti sono deterministici, la riconsegna è sicura.

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
                new MqttPahoMessageDrivenChannelAdapter(inboundClientId, mqttInboundClientFactory(), cloudResultTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttResultInboundChannel());
        return adapter;
    }

    // Canale in entrata per i risultati delle partite completate. Consuma lo stesso flusso durevole QoS1
    // usato dal servizio di statistiche. Serve per tracciare i gol validi solo per il torneo corrente, 
    // tenendoli separati dalla classifica globale. Ha un clientId dedicato per mantenere la propria coda.

    @Bean
    public MessageChannel mqttMatchResultInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttMatchResultInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                matchResultClientId, mqttInboundClientFactory(),
                it.uniupo.pissir.bitpub.common.constants.MqttTopics.CLOUD_MATCH_RESULT_TOPIC);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttMatchResultInboundChannel());
        return adapter;
    }

    // Sottoscrizione in entrata per i comandi di gestione tornei (Creazione/Aggiornamento/Eliminazione) inoltrati dall'Edge.
    // In questo modo la WebApp passa sempre dall'Edge invece di chiamare direttamente le API REST sul gateway cloud,
    // uniformando il comportamento con gli altri servizi. Usa una sottoscrizione durevole separata.

    @Bean
    public MessageChannel systemActionInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer systemActionInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(systemActionClientId, mqttInboundClientFactory(), systemActionTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(systemActionInboundChannel());
        return adapter;
    }
}
