package com.bitpub.utils;

import com.google.gson.*;
import com.bitpub.models.Partita;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility per la gestione del formato JSON.
 * Stefano Bellan
 */
public class JsonManager {
    private static JsonManager instance;
    private final Gson gson;

    // Formattatore standard ISO (es: 2026-05-03) per compatibilità con Spring Boot
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JsonManager() {
        this.gson = new GsonBuilder()
                // 1. RIMOSSO excludeFieldsWithoutExposeAnnotation per inviare tutti i dati del Torneo

                // 2. ADAPTER PER LOCALDATE: Gestisce la scrittura (serialize) e lettura (deserialize) delle date
                .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                    @Override
                    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.format(DATE_FORMATTER));
                    }
                })
                .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                    @Override
                    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return LocalDate.parse(json.getAsString(), DATE_FORMATTER);
                    }
                })
                .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                    @Override
                    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.format(DATETIME_FORMATTER));
                    }
                })
                .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                    @Override
                    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        if (json.isJsonObject() && json.getAsJsonObject().entrySet().isEmpty()) {
                            return null; // Handle "{}" gracefully
                        }
                        return LocalDateTime.parse(json.getAsString(), DATETIME_FORMATTER);
                    }
                })

                // 3. Manteniamo l'adattatore esistente per le partite
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