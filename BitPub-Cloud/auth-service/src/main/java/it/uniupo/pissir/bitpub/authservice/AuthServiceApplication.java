/**
 * Autore: Luca Franzon 20054744
 * Entry point del microservizio di autenticazione. 
 * Si occupa di inizializzare il contesto di Spring Boot e di importare 
 * le configurazioni di sicurezza e le utilità necessarie per la gestione dei token.
 */
package it.uniupo.pissir.bitpub.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;

// Importiamo anche il GlobalExceptionHandler per mappare la BitpubException su una risposta HTTP pulita (es. 401)
// evitando che l'eccezione risalga fino al dispatcher generando stack trace superflui nei log.
@SpringBootApplication(scanBasePackages = "it.uniupo.pissir.bitpub")
@Import({JwtUtils.class, GlobalExceptionHandler.class})
public class AuthServiceApplication {
    /**
     * Metodo di avvio dell'applicazione Spring Boot.
     *
     * @param args argomenti passati da riga di comando durante l'avvio
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
