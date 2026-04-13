package com.bitpub;

import com.bitpub.models.Locale;
import com.bitpub.utils.JsonManager;

/**
 * Classe principale per il test e l'avvio del sistema BitPub Cloud.
 * Dimostra il funzionamento della serializzazione degli oggetti in JSON.
 */
public class Main {
    public static void main(String[] args) {
        // Messaggio di log iniziale per verificare l'esecuzione
        System.out.println("--- Test Avvio BitPub Cloud ---");

        // Crea una nuova istanza di Locale usando il costruttore "pieno"
        Locale locale = new Locale("Sala Matrix", "192.168.1.2");
        // Imposta manualmente l'ID del locale
        locale.setId(1L);

        // Ottiene l'istanza configurata di Gson tramite la classe di utilità JsonManager
        // Converte l'oggetto 'locale' in una stringa in formato JSON
        String jsonResult = JsonManager.getGson().toJson(locale);

        // Stampa il risultato: verranno inclusi SOLO i campi con l'annotazione @Expose
        System.out.println("Oggetto convertito a JSON: " + jsonResult);
    }
}
