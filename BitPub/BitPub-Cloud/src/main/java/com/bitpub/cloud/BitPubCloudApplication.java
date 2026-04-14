package com.bitpub.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Questa annotazione magica dice a Spring: "Ehi, questa è un'app Spring Boot,
// configura tutto in automatico basandoti sulle dipendenze che abbiamo nel pom.xml!"
@SpringBootApplication
public class BitPubCloudApplication {

    public static void main(String[] args) {
        // Questo comando avvia il server integrato (Tomcat) e mette l'app in ascolto
        SpringApplication.run(BitPubCloudApplication.class, args);
        System.out.println("Modulo BitPub-Cloud avviato con successo! Le API REST sono pronte.");
    }
}