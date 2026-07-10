package it.uniupo.pissir.bitpub.localeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * Classe principale per l'avvio del microservizio dedicato ai locali.
 * Include la scansione dei componenti comuni di BitPub oltre a quelli specifici del servizio.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.localeservice", "it.uniupo.pissir.bitpub.common"})
public class LocaleServiceApplication {
    
    // Punto di ingresso dell'applicazione Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(LocaleServiceApplication.class, args);
    }
}
