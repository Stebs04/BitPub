package it.uniupo.pissir.bitpub.localeservice.config;

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
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Configurazione per il protocollo MQTT relativo al servizio dei locali.
 * - In uscita (outbound): pubblica eventi di aggiunta o rimozione dei giochi, 
 *   mantenendo sincronizzata la visuale locale del dispositivo Edge con il catalogo centrale in cloud.
 * - In ingresso (inbound): si iscrive ai comandi di gestione dei locali inoltrati dall'Edge, 
 *   permettendo cosi' una gestione centralizzata senza che l'applicazione web bypassi l'Edge stesso.
 */
@Configuration
public class MqttConfig {

    @Value("${bitpub.mqtt.cloud.broker-url}")
    private String brokerUrl;

    @Value("${bitpub.mqtt.cloud.client-id}")
    private String clientId;

    @Value("${bitpub.mqtt.cloud.inbound-client-id}")
    private String inboundClientId;

    @Value("${bitpub.mqtt.cloud.system-action-topic}")
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

    // ── Inbound: Comandi di gestione locale inoltrati dall'Edge ────────────────────────────────
    // Utilizziamo una sessione durevole (cleanSession=false e un clientId stabile) in modo che il broker 
    // metta in coda i comandi con QoS1 durante eventuali inattivita' del servizio, per poi riconsegnarli alla riconnessione.

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
