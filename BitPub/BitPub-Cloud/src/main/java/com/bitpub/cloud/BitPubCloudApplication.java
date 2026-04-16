package com.bitpub.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 1. Scansiona tutti i Component, Controller e Service nel progetto
@SpringBootApplication(scanBasePackages = "com.bitpub")

// 2. Dice ad Hibernate dove trovare le tue classi @Entity (Utente, Locale, ecc.)
@EntityScan(basePackages = "com.bitpub.models")

// 3. Dice a Spring Data JPA dove trovare i tuoi DAO/Repository
@EnableJpaRepositories(basePackages = "com.bitpub.repository")
public class BitPubCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(BitPubCloudApplication.class, args);
    }
}