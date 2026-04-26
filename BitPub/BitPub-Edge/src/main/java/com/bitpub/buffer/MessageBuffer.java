package com.bitpub.buffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffer di memoria thread-safe per la gestione temporanea dei messaggi MQTT.
 * <p>
 * Questa classe funge da intermediario tra i Subscriber locali (Producers) e i Task di inoltro
 * verso il Cloud (Consumers). Implementa una politica di gestione delle code di tipo
 * "Oldest Drop": se il buffer raggiunge la capacità massima, il messaggio più vecchio
 * viene rimosso per far spazio ai nuovi dati.
 * </p>
 * @author Timothy Giolito 20054431
 * @author Stefano Bellan 20054330 (integrazione e adattamento)
 * @modified Stefano Bellan 20054330 - Fase 25: aggiunto logging professionale
 */
public class MessageBuffer {

    private static final Logger logger = LoggerFactory.getLogger(MessageBuffer.class);
    
    // Costante di capacità inserita per gestire il buffer e percentuali
    private final int MAX_CAPACTITY = 50;

    /**
     * Coda bloccante sottostante per la gestione concorrente dei messaggi.
     * La dimensione è fissata a 50 per evitare un consumo eccessivo di memoria RAM.
     */
    private final BlockingQueue<String> codaMessaggi;

    /**
     * Costruttore predefinito.
     * Inizializza la coda con una capacità massima di 50 elementi.
     */
    public MessageBuffer() {
        this.codaMessaggi = new LinkedBlockingQueue<>(MAX_CAPACTITY);
    }

    /**
     * Restituisce la capacità massima del buffer.
     * 
     * @return La capacità in eventi.
     */
    public int getCapacita() {
        return MAX_CAPACTITY;
    }

    /**
     * Ritorna gli elementi presenti e pendenti.
     * @return Il numero di oggetti presenti.
     */
    public int getPendenti() {
        return codaMessaggi.size();
    }

    /**
     * Inserisce un nuovo messaggio nel buffer.
     * <p>
     * Se il buffer è pieno, il metodo rimuove automaticamente l'elemento in testa
     * (il più vecchio) per garantire che i dati più recenti vengano sempre memorizzati.
     * </p>
     *
     * @param messaggio La stringa (solitamente JSON) contenente i dati da bufferizzare.
     */
    public void push(String messaggio) {
       boolean successo = codaMessaggi.offer(messaggio); // prova a inserire 
       
       if(!successo){
           // Scarta l'evento più vecchio per far spazio alla queue circolare
           String scartato = codaMessaggi.poll();
           if(scartato != null) {
               logger.error("Buffer pieno - evento scartato: {}", scartato);
           }
           codaMessaggi.offer(messaggio); // Riprova l'inserimento
       }

       // Ad ogni inserimento nel buffer loggo lo stato
       logger.debug("Evento inserito nel buffer - dimensione attuale: {}/{}", codaMessaggi.size(), getCapacita());
       
       // Calcola percentuale di occupazione
       double percentuale = ((double) codaMessaggi.size() / getCapacita()) * 100;
       // Avviso del 80% circa sul buffer
       if (percentuale >= 80.0) {
           logger.warn("Buffer quasi pieno - occupazione: {}% ({}/{})", (int)percentuale, codaMessaggi.size(), getCapacita());
       }
    }

    /**
     * Restituisce il primo messaggio della coda senza rimuoverlo.
     *
     * @return Il messaggio in testa alla coda, oppure {@code null} se il buffer è vuoto.
     */
    public String peek(){
        return codaMessaggi.peek();
    }

    /**
     * Preleva e rimuove il primo messaggio dalla testa della coda.
     *
     * @return Il messaggio rimosso, o {@code null} se vuota.
     */
    public String poll(){
        String rs = codaMessaggi.poll();
        if (rs != null) {
            // Loggo ad estrazione avvenuta che la coda è ridotta
            logger.debug("Stato buffer - size: {}, capacita: {}, pendenti: {}", codaMessaggi.size(), getCapacita(), getPendenti());
        }
        return rs;
    }

    /**
     * Restituisce il numero di elementi attualmente presenti nel buffer.
     *
     * @return Il numero in int.
     */
    public int getDimensione() {
        return codaMessaggi.size();
    }
}
