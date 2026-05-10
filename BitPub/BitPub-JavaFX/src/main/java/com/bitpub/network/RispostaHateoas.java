package com.bitpub.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object (DTO) progettato per incapsulare e deserializzare le risposte 
 * ipermediali standard fornite dal backend Spring Boot. 
 */
public class RispostaHateoas {

    @Expose
    @SerializedName("_links")
    private Map<String, LinkDettaglio> _links;

    /**
     * Fornisce l'accesso al dizionario dei collegamenti ipermediali disponibili.
     */
    public Map<String, LinkDettaglio> getLinks() {
        if (_links == null) {
            _links = new HashMap<>();
        }
        return _links;
    }

    /**
     * Recupera l'URL di un link specifico per relazione, lanciando un'eccezione descrittiva se mancante.
     */
    public String getLinkSafe(String rel) {
        LinkDettaglio link = getLinks().get(rel);
        if (link == null) {
            throw new RuntimeException("Link HATEOAS '" + rel + "' non trovato nella risposta del server.");
        }
        return link.getHref();
    }

    public static class LinkDettaglio {

        @Expose
        private String href;

        public String getHref() {
            return href;
        }
    }
}