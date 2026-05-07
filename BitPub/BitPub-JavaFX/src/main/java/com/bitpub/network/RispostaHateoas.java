package com.bitpub.network;

import com.google.gson.annotations.Expose;
import java.util.Map;

/**
 * Data Transfer Object (DTO) progettato per incapsulare e deserializzare le risposte 
 * ipermediali standard fornite dal backend Spring Boot. 
 * Il suo scopo strutturale è fornire al RestClient un modello tipizzato per l'estrazione 
 * e l'analisi della mappa dei link (_links), abilitando di fatto la navigazione 
 * dinamica degli endpoint e l'implementazione del pattern architetturale HATEOAS.
 */
public class RispostaHateoas {

    // Mappa la struttura JSON nativa di Spring Data REST isolando il blocco _links,
    // dove la chiave identifica il tipo di relazione (rel) e il valore l'indirizzo
    @Expose
    private Map<String, LinkDettaglio> _links;

    /**
     * Fornisce l'accesso al dizionario dei collegamenti ipermediali disponibili.
     * 
     * @return La mappa delle relazioni scoperte a runtime per la risorsa interrogata
     */
    public Map<String, LinkDettaglio> getLinks() {
        return _links;
    }

    /**
     * Sottoclasse statica che riflette la struttura formale di un singolo nodo di navigazione.
     * Permette alla libreria Gson di isolare e proiettare in memoria gli attributi 
     * specifici del link omettendo eventuali metadati aggiuntivi ignorati dal client.
     */
    public static class LinkDettaglio {
        
        // Proprietà fondamentale che conserva l'URI (Uniform Resource Identifier)
        // a cui la logica di business dovrà indirizzare la successiva transazione HTTP
        @Expose
        private String href;

        /**
         * Restituisce l'indirizzo operativo associato alla specifica relazione.
         * 
         * @return L'URL assoluto o relativo pronto per essere processato dal RestClient
         */
        public String getHref() {
            return href;
        }
    }
}