package com.bitpub.utils; // Assicurati che il package sia corretto in base al tuo progetto

/**
 * Classe di utilità per gestire l'albero dei topic MQTT relativi al modulo Freccette.
 */
public class MqttFreccetteTopics {

    // --- TOPIC PER L'ISCRIZIONE (SUBSCRIBE) ---
    // Usato dal Backend/Edge per ascoltare TUTTI gli eventi di TUTTI i bersagli
    public static final String WILDCARD_SCORE = "bitpub/locali/+/freccette/+/score";
    public static final String WILDCARD_EVENTI = "bitpub/locali/+/freccette/+/eventi";

    // --- METODI PER LA PUBBLICAZIONE (PUBLISH) ---
    // Usato dal Simulatore Freccette per generare il topic specifico del suo bersaglio

    /**
     * Genera il topic per inviare un aggiornamento del punteggio.
     * @param idLocale L'identificativo del pub (es. "pub_milano_01")
     * @param idDispositivo L'identificativo del bersaglio (es. "bersaglio_A")
     * @return Stringa formattata (es. "bitpub/locali/pub_milano_01/freccette/bersaglio_A/score")
     */
    public static String getScoreTopic(String idLocale, String idDispositivo) {
        return "bitpub/locali/" + idLocale + "/freccette/" + idDispositivo + "/score";
    }

    /**
     * Genera il topic per inviare eventi generici (es. inizio partita, fine partita).
     */
    public static String getEventiTopic(String idLocale, String idDispositivo) {
        return "bitpub/locali/" + idLocale + "/freccette/" + idDispositivo + "/eventi";
    }
}