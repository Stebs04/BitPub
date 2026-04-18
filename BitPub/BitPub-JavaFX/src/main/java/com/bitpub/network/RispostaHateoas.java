package com.bitpub.network;

import java.util.Map;

// Questa classe avvolge i tuoi dati (es. Partita) e cattura i link HATEOAS
public class RispostaHateoas<T> {

    // I dati veri e propri (es. la lista delle partite o un singolo locale)
    private T data;

    // Mappa che conterrà l'oggetto "_links" generato da Spring
    // La chiave è il nome dell'azione (es. "self", "update", "delete")
    // Il valore è il link vero e proprio
    private Map<String, LinkDettaglio> _links;

    public T getData() { return data; }
    public Map<String, LinkDettaglio> getLinks() { return _links; }

    // Sotto-classe per leggere la struttura esatta dei link di Spring HATEOAS
    public static class LinkDettaglio {
        private String href;
        public String getHref() { return href; }
    }
}