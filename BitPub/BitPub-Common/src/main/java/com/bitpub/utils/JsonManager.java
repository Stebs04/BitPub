package com.bitpub.utils;

import com.google.gson.Gson;

/**
 * Utility per la gestione del formato JSON.
 * Stefano Bellan
 */
public class JsonManager {
    private static JsonManager instance;
    private final Gson gson;

    // Il costruttore deve essere privato per il Singleton
    private JsonManager() {
        this.gson = new Gson();
    }

    // Questo è il metodo che manca e causa l'errore nel RestClient
    public static synchronized JsonManager getInstance() {
        if (instance == null) {
            instance = new JsonManager();
        }
        return instance;
    }

    public String toJson(Object obj) { 
        return gson.toJson(obj); 
    }
    
    public <T> T fromJson(String json, Class<T> classOfT) { 
        return gson.fromJson(json, classOfT); 
    }
}