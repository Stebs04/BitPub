/**
 * autore Timothy Giolito 20054431
 * Classe principale per l'avvio dell'applicazione Spring Boot.
 * Abilita la schedulazione per le operazioni periodiche dei simulatori.
 */
package it.uniupo.pissir.bitpub.simulators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoControlPanelApplication {
    public static void main(String[] args) {
        // Avvio del contesto applicativo Spring Boot
        SpringApplication.run(DemoControlPanelApplication.class, args);
    }
}
