package com.bitpub.utils;

import com.google.gson.*;
import com.bitpub.models.*; // Importante: importa i nuovi modelli
import java.lang.reflect.Type;

/**
 * Questa classe insegna a GSON come deserializzare oggetti polimorfici di tipo Partita.
 */
public class PartitaDeserializer implements JsonDeserializer<Partita> {

    @Override
    public Partita deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement tipoElement = jsonObject.get("tipoGioco");

        if (tipoElement == null) {
            throw new JsonParseException("Errore: Campo 'tipoGioco' mancante. Impossibile smistare la partita.");
        }

        String tipo = tipoElement.getAsString();

        switch (tipo.toUpperCase()) {
            case "BILIARDO":
                return context.deserialize(jsonObject, PartitaBiliardo.class);

            case "FRECCETTE":
                return context.deserialize(jsonObject, PartitaFreccette.class);

            case "CALCIOBALILLA":
                // Ora GSON sa gestire anche il Calciobalilla!
                return context.deserialize(jsonObject, PartitaCalciobalilla.class);

            default:
                throw new JsonParseException("Tipo di gioco sconosciuto: " + tipo);
        }
    }
}