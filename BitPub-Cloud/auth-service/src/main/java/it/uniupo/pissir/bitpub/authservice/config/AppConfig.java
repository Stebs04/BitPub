/**
 * Autore: Luca Franzon 20054744
 * Classe di configurazione che si occupa di istanziare e rendere disponibili 
 * i componenti infrastrutturali necessari al servizio, come i client HTTP.
 */
package it.uniupo.pissir.bitpub.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /**
     * Configura e fornisce un'istanza di RestClient pronta per effettuare 
     * chiamate HTTP sincrone verso altri servizi dell'architettura.
     *
     * @param builder costruttore fornito dal framework Spring
     * @return un'istanza configurata di RestClient
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
