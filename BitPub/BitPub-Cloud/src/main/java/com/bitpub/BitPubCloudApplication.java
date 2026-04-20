package com.bitpub.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto di ingresso principale per BitPub Cloud.
 * Questa classe sostituisce ogni altro Main e avvia l'intero ecosistema.
 */
@SpringBootApplication(scanBasePackages = "com.bitpub") // 1. Scansiona Controller, Service e il Gateway MQTT
@EntityScan(basePackages = "com.bitpub.models")        // 2. Collega le classi Entity per PostgreSQL
@EnableJpaRepositories(basePackages = "com.bitpub.repository") // 3. Attiva i DAO/Repository
@EnableAsync // 4. FONDAMENTALE: Attiva l'elaborazione asincrona per il salvataggio dei messaggi MQTT
public class BitPubCloudApplication {

    public static void main(String[] args) {
        // Avvio del framework Spring Boot
        SpringApplication.run(BitPubCloudApplication.class, args);

        System.out.println("=================================================");
        System.out.println("   BITPUB CLOUD: SISTEMA ATTIVO E INTEGRATO      ");
        System.out.println("   - Database: Collegato su localhost:5432 ");
        System.out.println("   - MQTT: Gateway in ascolto             ");
        System.out.println("   - Security: Architettura Stateless attiva");
        System.out.println("=================================================");
    }
}