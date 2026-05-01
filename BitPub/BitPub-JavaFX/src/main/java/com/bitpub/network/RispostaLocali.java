package com.bitpub.network;

import com.bitpub.models.Locale;

/**
 * Classe "contenitore" per leggere la risposta HATEOAS del server.
 * Il server restituisce un oggetto JSON con un campo "content" che contiene l'array di locali.
 */
public class RispostaLocali {
    
    // Questo campo deve chiamarsi ESATTAMENTE come nel JSON del server ("content")
    private Locale[] content;

    // Costruttore vuoto necessario per la conversione JSON (Gson)
    public RispostaLocali() {
    }

    public Locale[] getContent() {
        return content;
    }

    public void setContent(Locale[] content) {
        this.content = content;
    }
}