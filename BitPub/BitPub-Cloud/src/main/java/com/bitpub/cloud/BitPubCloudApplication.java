package com.bitpub.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan; // Aggiungi questa

@SpringBootApplication
@EntityScan("com.bitpub.models") // Dice a Spring di guardare dentro models per creare le tabelle!
public class BitPubCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitPubCloudApplication.class, args);
        System.out.println("Modulo BitPub-Cloud avviato con successo!");
    }
}