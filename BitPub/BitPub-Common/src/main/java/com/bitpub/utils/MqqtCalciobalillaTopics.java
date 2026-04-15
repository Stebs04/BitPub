package com.bitpub.utils;

/**
 * Utility class per la gestione della gerarchia dei topic MQTT relativi al sistema Calciobalilla.
 * <p>
 * Definisce le costanti per i path e i metodi helper per generare i topic di pubblicazione
 * e sottoscrizione (subscription) utilizzati nella comunicazione tra Edge e Cloud.
 * </p>
 *
 * @author Stefano Bellan 20054330
 */
public class MqqtCalciobalillaTopics {

    // Root principale della gerarchia MQTT per l'ecosistema BitPub.
    public static final String ROOT = "bitpub/locali";

    //Segmento di path identificativo per il sottosistema Calciobalilla.
    public static final String GIOCO = "/calciobalilla/";

    //Segmento finale per i topic relativi ai flussi di eventi.
    public static final String EVENTI = "/eventi";

    /**
     * Pattern globale per la sottoscrizione cloud-side.
     * Utilizza i wildcard '+' per intercettare gli eventi di tutti i locali e dispositivi.
     */
    public static final String TOPIC_CLOUD = "bitpub/locali/+/calciobalilla/+/eventi";

    /**
     * Genera il topic specifico per la pubblicazione degli eventi di un singolo dispositivo.
     * <br>Formato: {@code bitpub/locali/{idLocale}/calciobalilla/{idDispositivo}/eventi}
     *
     * @param idLocale      Identificativo univoco del locale.
     * @param idDispositivo Identificativo univoco dell'hardware (tavolo da gioco).
     * @return Stringa formattata del topic MQTT per la pubblicazione.
     */
    public static String getTopicPubblicazione(String idLocale, String idDispositivo) {
        // Combina i parametri per creare un path granulare (leaf node)
        return String.format("bitpub/locali/%s/calciobalilla/%s/eventi", idLocale, idDispositivo);
    }

    /**
     * Genera il topic per la sottoscrizione da parte dei gateway locali (Edge).
     * Permette di ascoltare gli eventi di tutti i dispositivi presenti in un determinato locale.
     *
     * @param idLocale Identificativo univoco del locale.
     * @return Stringa del topic contenente il wildcard '+' per l'ID dispositivo.
     */
    public static String getTopicSottoscrizioneEdge(String idLocale) {
        // Utilizziamo il wildcard '+' per monitorare tutti i dispositivi sotto lo stesso locale
        return String.format("bitpub/locali/%s/calciobalilla/+/eventi", idLocale);
    }
}
