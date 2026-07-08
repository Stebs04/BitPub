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

    // ── Outbound: publish live TournamentDto updates to the WebApp (bitpub/tournaments/{id}/state) ──

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
        handler.setDefaultTopic("bitpub/tournaments/unknown/state");
        return handler;
    }

    // ── Inbound: durable subscriber to Edge-forwarded tournament results (QoS1, cleanSession=false) ──
    // Stable clientId + durable session so the broker queues results while this service is down and
    // redelivers on reconnect. updateMatchResult sets deterministic values, so a redelivery is safe.

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

    // ── Inbound: completed-match results (bitpub/cloud/matches/result), the same durable QoS1 stream
    // statistics-service consumes. Source of tournament-isolated goals: only bracket matches update
    // per-tournament goals, decoupled from the global leaderboard. Distinct clientId = own subscription. ──

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
}
