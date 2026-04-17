package it.bitpub.edge.mqtt;

/**
 * Costanti per l'albero dei topic MQTT relativi al Biliardo.
 * ROOT: bitpub/locali/+/biliardo/+/
 */
public class BiliardoTopicConstants {
    // Topic specifico richiesto dai requisiti per le palle in buca
    public static final String TOPIC_IMBUCATE = "bitpub/locali/+/biliardo/+/imbucate";

    // Altri topic utili per il dominio del biliardo
    public static final String TOPIC_FALLI = "bitpub/locali/+/biliardo/+/falli";
    public static final String TOPIC_STATO_PARTITA = "bitpub/locali/+/biliardo/+/stato";
}