package it.uniupo.pissir.bitpub.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * Classe principale delegata all'avvio del microservizio dedicato alla gestione delle notifiche.
 * L'annotazione di Spring Boot si occupa di configurare automaticamente i componenti necessari
 * al corretto funzionamento dell'applicazione nel suo contesto di esecuzione.
 */
@SpringBootApplication
public class NotificationServiceApplication {
    
    /**
     * Punto di ingresso standard dell'applicazione Java. Inizializza e lancia il contesto 
     * di Spring Boot, mettendo in ascolto il servizio per le richieste in ingresso.
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
