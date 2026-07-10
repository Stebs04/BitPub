package it.uniupo.pissir.bitpub.userservice;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Classe di avvio principale per il servizio utenti (User Service).
 * Inizializza l'applicazione Spring Boot e configura la scansione dei componenti
 * per includere sia il pacchetto corrente che le classi condivise (bitpub-common),
 * gestendo opportunamente i filtri per non compromettere i test di slice.
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

// The explicit scan pulls in the shared bitpub-common beans (@SpringBootApplication alone would only
// scan this package). The two exclude filters restore what the default scan applies automatically —
// without them an explicit @ComponentScan breaks @WebMvcTest/@DataJpaTest slices (they'd load every
// @Component, e.g. the MQTT listener and data seeder).
@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.userservice", "it.uniupo.pissir.bitpub.common"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
        })
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
