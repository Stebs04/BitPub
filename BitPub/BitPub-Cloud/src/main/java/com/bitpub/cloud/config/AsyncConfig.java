package com.bitpub.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configurazione per la gestione della concorrenza asincrona.
 * @EnableAsync attiva la capacità di Spring di eseguire metodi in background.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mqttDbTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Numero di thread sempre attivi (i nostri lavoratori di base)
        executor.setCorePoolSize(10);
        // Numero massimo di thread se c'è un picco di dati in arrivo
        executor.setMaxPoolSize(50);
        // Dimensione della coda di attesa prima di rifiutare i messaggi
        executor.setQueueCapacity(1000);
        // Diamo un nome ai thread per facilitare la lettura dei log
        executor.setThreadNamePrefix("MqttDbWorker-");
        executor.initialize();

        System.out.println("[AsyncConfig] Thread Pool per salvataggi DB inizializzato.");
        return executor;
    }
}