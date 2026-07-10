// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice;

import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

// Registriamo il GlobalExceptionHandler per mappare correttamente le eccezioni specifiche (BitpubException)
// sui codici di stato HTTP appropriati. Senza questa configurazione, eventuali rifiuti logici 
// (come un'azione fuori turno) verrebbero tradotti in errori generici 500, causando ritentativi 
// non voluti da parte del componente Edge.
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class MatchServiceApplication {

    // Punto di ingresso dell'applicazione Spring Boot per la gestione delle partite
    public static void main(String[] args) {
        SpringApplication.run(MatchServiceApplication.class, args);
    }
}
