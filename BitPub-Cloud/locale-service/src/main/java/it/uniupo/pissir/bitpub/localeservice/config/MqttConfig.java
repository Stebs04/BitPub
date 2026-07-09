package it.uniupo.pissir.bitpub.localeservice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Outbound-only MQTT: publishes game ADD/REMOVE events so the Edge node keeps its
 * local view of installed game instances in sync with the cloud catalog.
 */
@Configuration
public class MqttConfig {

    @Value("${bitpub.mqtt.cloud.broker-url}")
    private String brokerUrl;

    @Value("${bitpub.mqtt.cloud.client-id}")
    private String clientId;

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
    public MessageChannel gameOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "gameOutboundChannel")
    public MessageHandler gameOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }

    @MessagingGateway(defaultRequestChannel = "gameOutboundChannel")
    public interface GamePublisher {
        void publish(String payload, @Header(MqttHeaders.TOPIC) String topic);
    }
}
