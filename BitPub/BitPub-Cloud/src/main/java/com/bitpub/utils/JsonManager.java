package com.bitpub.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Classe di utilità per la gestione della serializzazione e deserializzazione JSON.
 * Configura il motore Gson per rispettare le annotazioni di visibilità dei campi.
 *
 * @author Stefano Bellan 20054330
 */
public class JsonManager {

    /**
     * Crea e restituisce un'istanza configurata di Gson.
     *
     * @return Un oggetto Gson che processa solo i campi annotati con @Expose.
     */
    public static Gson getGson() {
        // Usa il Builder per personalizzare il comportamento di Gson
        return new GsonBuilder()
                // Fondamentale: ignora tutti i campi che NON hanno l'annotazione @Expose
                .excludeFieldsWithoutExposeAnnotation()
                // Finalizza la configurazione e crea l'oggetto pronto all'uso
                .create();
    }
}
