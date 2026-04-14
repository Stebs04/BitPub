import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * Questa classe insegna a GSON come deserializzare oggetti polimorfici di tipo Partita.
 */
public class PartitaDeserializer implements JsonDeserializer<Partita> {

    @Override
    public Partita deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Convertiamo l'elemento in un oggetto JSON per poterne leggere i campi
        JsonObject jsonObject = json.getAsJsonObject();

        // Cerchiamo il campo che ci dice che tipo di gioco è.
        // ATTENZIONE: Assicurati con Timothy che il campo si chiami esattamente "tipoGioco"
        JsonElement tipoElement = jsonObject.get("tipoGioco");

        if (tipoElement == null) {
            throw new JsonParseException("Errore: Campo 'tipoGioco' mancante. Impossibile capire se è Biliardo o Freccette.");
        }

        // Estraiamo il valore come stringa (es. "BILIARDO" o "FRECCETTE")
        String tipo = tipoElement.getAsString();

        // Smistiamo la deserializzazione in base al tipo
        switch (tipo.toUpperCase()) {
            case "BILIARDO":
                // Diciamo a GSON di trattare questo JSON come una PartitaBiliardo
                return context.deserialize(jsonObject, PartitaBiliardo.class);

            case "FRECCETTE":
                // Diciamo a GSON di trattare questo JSON come una PartitaFreccette
                return context.deserialize(jsonObject, PartitaFreccette.class);

            default:
                throw new JsonParseException("Tipo di gioco sconosciuto: " + tipo);
        }
    }
}