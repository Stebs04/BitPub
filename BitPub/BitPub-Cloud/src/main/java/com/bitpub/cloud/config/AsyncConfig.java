package com.bitpub.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configurazione avanzata della concorrenza per BitPub Cloud.
 * Risolve la perdita del contesto di sicurezza tra thread e implementa
 * una policy di backpressure per la resilienza dei dati MQTT.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mqttDbTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // --- Dimensionamento del Pool ---
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("MqttDbWorker-");

        /**
         * REF: CallerRunsPolicy
         * Se la coda è piena, il thread che invoca il task (es. il Gateway MQTT)
         * eseguirà lui stesso il salvataggio. Questo crea un naturale 'backpressure',
         * rallentando la ricezione ma garantendo ZERO PERDITE di log.
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        /**
         * REF: DelegatingSecurityContextAsyncTaskExecutor
         * Avvolge l'executor per copiare automaticamente il SecurityContext (JWT/User)
         * dal thread principale al thread worker asincrono.
         */
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}