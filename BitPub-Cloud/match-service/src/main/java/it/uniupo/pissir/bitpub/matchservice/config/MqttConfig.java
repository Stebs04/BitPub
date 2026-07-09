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

    // ── Outbound: publish game-state updates to the WebApp ──────────────────────────

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

    // ── Inbound: subscribe to Edge-forwarded sensor events ──────────────────────────
    // Durable session (cleanSession=false + stable clientId) so the broker queues QoS1 events
    // while match-service is down and redelivers them on reconnect — this replaces the Edge's
    // app-level sensor buffer. processSensorEvent is idempotent on eventId, so redelivery is safe.
    // ponytail: broker holds the queue in memory; enable mosquitto `persistence true` if the queue
    // must survive a broker restart too.

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

    // ── Inbound: subscribe to Edge-forwarded interactive game actions ────────────────
    // Separate durable subscriber (distinct clientId, own channel) from the sensor one so the
    // CommandIngestListener parses only command payloads. Same QoS1 durable-queue guarantee.

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

    // ── Inbound: subscribe to Edge-forwarded final match results ─────────────────────
    // The Edge reports the enriched final result (players, exact scores, winner) here instead of the
    // former synchronous REST POST /result. Durable QoS1 subscriber, so a result buffered by an
    // offline Edge and flushed on reconnect is persisted. applyFinalResult is idempotent on matchId.

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
