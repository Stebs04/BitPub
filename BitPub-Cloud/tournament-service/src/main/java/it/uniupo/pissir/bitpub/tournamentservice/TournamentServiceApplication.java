/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale che avvia il microservizio dedicato alla gestione dei tornei.
 * Inizializza il contesto Spring Boot e fa partire l'applicazione.
 */
@SpringBootApplication
public class TournamentServiceApplication {
    // Punto di ingresso standard per le applicazioni Spring Boot
    public static void main(String[] args) {
        SpringApplication.run(TournamentServiceApplication.class, args);
    }
}
