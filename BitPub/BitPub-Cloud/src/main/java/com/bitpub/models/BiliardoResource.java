package com.bitpub.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe per rappresentare un singolo link HATEOAS.
 */
class Link {
    private String href;

    public Link(String href) {
        this.href = href;
    }
}

/**
 * Risorsa specifica per il Biliardo con supporto HATEOAS.
 */

public class ResourceSupport {
    // Dati dell'evento (es. palla numero 8 in buca)
    private String idEvento;
    private String tipoAzione;
    private String idSquadra;
    private String idMatch;

    private Map<String, Link> _links = new HashMap<>();


    public ResourceSupport(String idEvento, String tipoAzione, String idSquadra, String idMatch) {
        this.idEvento = idEvento;
        this.tipoAzione = tipoAzione;
        this.idSquadra = idSquadra;
        this.idMatch = idMatch;

        // Aggiunta dinamica dei percorsi ipertestuali
        this.addLink("self", "/api/v1/biliardo/eventi/" + idEvento);
        this.addLink("dettagli_squadra", "/api/v1/squadre/" + idSquadra);
        this.addLink("prossimo_match", "/api/v1/partite/prossime/" + idMatch);
    }

    public void addLink(String rel, String href) {
        _links.put(rel, new Link(href));
    }
}