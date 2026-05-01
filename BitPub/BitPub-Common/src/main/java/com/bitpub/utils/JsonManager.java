package com.bitpub.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bitpub.models.Partita;

/**
 * Utility per la gestione del formato JSON.
 * Stefano Bellan
 */
public class JsonManager {
    private static JsonManager instance;
    private final Gson gson;

    // Il costruttore deve essere privato per il Singleton
    private JsonManager() {
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Partita.class, new PartitaDeserializer())
                .create();
    }

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

    public static Gson getGson() {
        return getInstance().gson;
    }
}