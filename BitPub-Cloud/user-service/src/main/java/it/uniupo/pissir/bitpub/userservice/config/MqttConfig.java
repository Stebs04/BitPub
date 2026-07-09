package it.uniupo.pissir.bitpub.userservice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

/**
 * Inbound-only MQTT: subscribes to Edge-forwarded user CUD commands so the WebApp no longer bypasses
 * the Edge for user management. Durable session (cleanSession=false + stable clientId) so the broker
 * queues QoS1 commands while user-service is down and redelivers them on reconnect.
 * See {@link SystemActionCommandListener} for the message handling.
 */
@Configuration
public class MqttConfig {

    @Value("${bitpub.mqtt.cloud.broker-url}")
    private String brokerUrl;

    @Value("${bitpub.mqtt.cloud.inbound-client-id}")
    private String inboundClientId;

    @Value("${bitpub.mqtt.cloud.system-action-topic}")
    private String systemActionTopic;

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
