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
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JwtUtils.class)
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
