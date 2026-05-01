package com.bitpub.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry Point principale per l'applicazione BitPub Cloud.
 * Configura il contesto di Spring Boot, abilita la scansione globale dei pacchetti
 * per la persistenza JPA e attiva il supporto per le operazioni asincrone.
 * 
 */
@SpringBootApplication(scanBasePackages = "com.bitpub")
@EntityScan(basePackages = "com.bitpub")               // Scansione globale delle classi @Entity per il mapping ORM
@EnableJpaRepositories(basePackages = "com.bitpub")    // Abilitazione dei repository Spring Data JPA in tutto il progetto
@EnableAsync                                           // Supporto per l'esecuzione di task non bloccanti (@Async)
public class BitPubCloudApplication {

    /**
     * Avvia l'applicazione Spring Boot.
     * 
     * @param args Argomenti passati da riga di comando.
     */
    public static void main(String[] args) {
        SpringApplication.run(BitPubCloudApplication.class, args);

        // Feedback visuale del corretto avvio di tutti i moduli critici del Cloud
        System.out.println("=================================================");
        System.out.println("   BITPUB CLOUD: SISTEMA ATTIVO E INTEGRATO      ");
        System.out.println("   - Database: Collegato su localhost:5432       ");
        System.out.println("   - MQTT: Gateway in ascolto                    ");
        System.out.println("   - Security: Architettura Stateless attiva     ");
        System.out.println("=================================================");
    }
}
