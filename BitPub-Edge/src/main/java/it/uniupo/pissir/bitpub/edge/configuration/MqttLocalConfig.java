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

    // Stable client id so the durable cloud subscriber (match-service) keeps its QoS1 queue.
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

    // ── Cloud egress: publish validated sensor events to the Cloud ingest topic (QoS1). ──
    // Same broker as local in this deployment; a separate handler/client keeps the roles distinct.

    @Bean
    public MessageChannel cloudMqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "cloudMqttOutboundChannel")
    public MessageHandler cloudMqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(cloudClientId, mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1); // QoS1 so the broker queues for the durable cloud subscriber while it is down
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

    // ── Cloud ingress: subscribe to the Cloud match-state sync push (Cloud -> Edge). ──
    // On IN_PROGRESS the Cloud publishes the full match state here; the Edge eagerly initializes its
    // authoritative LocalMatchState from this push instead of a REST pull, so it runs autonomously
    // while the Cloud is unreachable. Same broker as local in this deployment.

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

    // ── Cloud ingress: subscribe to game ADD/REMOVE events for this locale. ──

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
