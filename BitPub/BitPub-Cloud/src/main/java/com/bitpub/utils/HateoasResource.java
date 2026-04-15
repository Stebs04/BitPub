package com.bitpub.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe Contenitore per implementare HATEOAS.
 * Incapsula i dati (payload) e aggiunge il nodo _links richiesto.
 */
public class HateoasResource<T> {

    // I dati veri e propri (Es. il Torneo o il Log delle Freccette)
    private T data;

    // La mappa che diventerà il nodo JSON "_links"
    private Map<String, String> _links;

    public HateoasResource(T data) {
        this.data = data;
        this._links = new HashMap<>();
    }

    /**
     * Metodo per aggiungere un link alla risorsa.
     * @param nomeAzione Il nome dell'azione (es. "self", "dettagli_squadra")
     * @param url Il percorso API (es. "/api/tornei/5")
     */
    public void addLink(String nomeAzione, String url) {
        this._links.put(nomeAzione, url);
    }

    // Getter necessari a GSON/Jackson per convertire l'oggetto in JSON
    public T getData() {
        return data;
    }

    public Map<String, String> get_links() {
        return _links;
    }
}