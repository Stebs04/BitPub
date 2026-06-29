package com.bitpub.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility per estrarre liste di oggetti da risposte HATEOAS (HAL o Page/List standard).
 */
public class HateoasUtils {

    /**
     * Analizza l'albero JSON per supportare lo standard HAL (Hypertext Application Language)
     * utilizzato da Spring Data REST o un normale array "content".
     *
     * @param response L'oggetto JSON grezzo ricevuto dal backend
     * @param clazz La classe del DTO da mappare
     * @param gson Istanza di Gson da utilizzare
     * @return Una lista tipizzata di istanze
     */
    public static <T> List<T> extractArrayFromHateoas(JsonObject response, Class<T> clazz, Gson gson) {
        List<T> items = new ArrayList<>();
        try {
            if (response.has("_embedded")) {
                JsonObject embedded = response.getAsJsonObject("_embedded");
                String key = embedded.keySet().iterator().next();
                JsonArray array = embedded.getAsJsonArray(key);
                for (JsonElement el : array) {
                    items.add(gson.fromJson(el, clazz));
                }
            } else if (response.has("content")) {
                JsonArray array = response.getAsJsonArray("content");
                for (JsonElement el : array) {
                    items.add(gson.fromJson(el, clazz));
                }
            }
        } catch (Exception e) {
            System.err.println("Errore parsing HATEOAS array: " + e.getMessage());
        }
        return items;
    }
}
