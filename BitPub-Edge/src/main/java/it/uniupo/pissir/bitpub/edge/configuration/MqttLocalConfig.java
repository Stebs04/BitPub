/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
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

@Configuration
@IntegrationComponentScan
public class MqttLocalConfig {

    @Value("${bitpub.mqtt.local.broker-url}")
    private String brokerUrl;

    @Value("${bitpub.mqtt.local.client-id}")
    private String clientId;

    @Value("${bitpub.mqtt.local.topic-sensors}")
    private String topicSensors;

    // Uso un client id stabile così il subscriber cloud (match-service) mantiene in piedi la sua coda QoS1.
    @Value("${bitpub.mqtt.cloud.client-id}")
    private String cloudClientId;

    @Value("${bitpub.mqtt.cloud.topic-games}")
    private String topicGames;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    // Egress verso il Cloud: pubblico gli eventi sensore validati sul topic di ingest cloud in QoS1.
    // Anche se in questo rilascio il broker è lo stesso, tengo separati handler e client per mantenere distinti i ruoli.

    @Bean
    public MessageChannel cloudMqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "cloudMqttOutboundChannel")
    public MessageHandler cloudMqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(cloudClientId, mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1); // Manteniamo QoS1 per far accodare i messaggi al broker nel caso in cui il subscriber cloud cada
        return handler;
    }

    // Egress locale: pubblico le azioni di gioco direttamente verso il simulatore locale in QoS1, senza passare dal buffer cloud.
    // Questo permette a EdgeCommandController.gameAction di pilotare il simulatore direttamente quando l'Edge è offline,
    // evitando di accodare un'azione verso un Cloud che tanto non la riceverebbe.
    @Bean
    public MessageChannel localMqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "localMqttOutboundChannel")
    public MessageHandler localMqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-sim-action", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-" + java.util.UUID.randomUUID().toString(), mqttClientFactory(), topicSensors);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    // Ingress dal Cloud: mi iscrivo al push di sincronizzazione dello stato partita (Cloud -> Edge).
    // Quando lo stato passa a IN_PROGRESS, il Cloud spinge qui tutto lo stato; l'Edge lo usa per inizializzare subito
    // il suo LocalMatchState autoritativo invece di fare una chiamata REST, così può continuare a funzionare da solo
    // anche se il Cloud risulta irraggiungibile. In questa configurazione il broker è sempre quello locale.

    @Bean
    public MessageChannel matchSyncInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer matchSyncInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-sync-" + java.util.UUID.randomUUID().toString(),
                        mqttClientFactory(), "bitpub/edge/matches/+/sync");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(matchSyncInputChannel());
        return adapter;
    }

    // Ingress dal Cloud: sottoscrizione agli eventi ADD/REMOVE dei giochi per questo locale.

    @Bean
    public MessageChannel cloudGamesInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer gamesInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-games-" + java.util.UUID.randomUUID().toString(), mqttClientFactory(), topicGames);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(cloudGamesInputChannel());
        return adapter;
    }
}
