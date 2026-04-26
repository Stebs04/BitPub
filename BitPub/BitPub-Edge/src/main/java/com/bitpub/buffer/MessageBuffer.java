package com.bitpub.buffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 */
public class MessageBuffer {

    /**
     * Coda bloccante sottostante per la gestione concorrente dei messaggi.
     * La dimensione è fissata a 50 per evitare un consumo eccessivo di memoria RAM sull'Edge.
     */
    private final BlockingQueue<String> codaMessaggi;

    /**
     * Costruttore predefinito.
     * Inizializza la coda con una capacità massima di 50 elementi.
     */
    public MessageBuffer() {
        // LinkedBlockingQueue non ha limiti di capienza (a meno che non venga specificato)
        this.codaMessaggi = new LinkedBlockingQueue<>(50);
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
       boolean successo = codaMessaggi.offer(messaggio); //offer prova a inserire ma se la coda è piena restituisce false

       if(!successo){
        System.out.println("[WARNING] Coda Piena! Sacrifico l'evento più vecchio");
        codaMessaggi.poll(); //Rimuovo l'evento in testa che è fisicamente il più vecchio
        codaMessaggi.offer(messaggio); //Riprovo l'inserimento di un nuovo messaggio
       }

       System.out.println("[EDGE BUFFER] Log Aggiunto in coda" + codaMessaggi.size());
    }

    /**
     * Restituisce il primo messaggio della coda senza rimuoverlo.
     * Utile per ispezionare il prossimo dato da elaborare.
     *
     * @return Il messaggio in testa alla coda, oppure {@code null} se il buffer è vuoto.
     */
    public String peek(){
        return codaMessaggi.peek();
    }

    /**
     * Preleva e rimuove il primo messaggio dalla testa della coda.
     *
     * @return Il messaggio rimosso, oppure {@code null} se la coda è vuota.
     */
    public String poll(){
        return codaMessaggi.poll();
    }

    /**
     * Verifica se il buffer contiene messaggi.
     *
     * @return {@code true} se la coda non contiene elementi, {@code false} altrimenti.
     */
    public int getDimensione() {
        return codaMessaggi.size();
    }
}