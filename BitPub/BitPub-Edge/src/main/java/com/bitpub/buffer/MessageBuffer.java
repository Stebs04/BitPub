package com.bitpub.buffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Buffer basato su LinkedBlockingQueue.
 * Zero 'synchronized': la sicurezza concorrente è gestita nativamente da Java.
 * @author Stefano Bellan 20054330, Timothy Giolito
 */
public class MessageBuffer {

    // Utilizziamo BlockingQueue al posto di Queue
    private final BlockingQueue<String> codaMessaggi;

    public MessageBuffer() {
        // LinkedBlockingQueue non ha limiti di capienza (a meno che non venga specificato)
        this.codaMessaggi = new LinkedBlockingQueue<>(50);
    }

    // Usato da Stefano (Producer)
    public void push(String messaggio) {
       boolean successo = codaMessaggi.offer(messaggio); //offer prova a inserire ma se la coda è piena restituisce false

       if(!successo){
        System.out.println("[WARNING] Coda Piena! Sacrifico l'evento più vecchio");
        codaMessaggi.poll(); //Rimuovo l'evento in testa che è fisicamente il più vecchio
        codaMessaggi.offer(messaggio); //Riprovo l'inserimento di un nuovo messaggio
       }

       System.out.println("[EDGE BUFFER] Log Aggiunto in coda" + codaMessaggi.size());
    }

    /* 
    // Usato da te, Timothy (Consumer)
    // Usiamo take() che lancia InterruptedException se il thread viene fermato mentre aspetta
    public String take() throws InterruptedException {
        // take() mette in pausa il thread se la coda è vuota, finché non arriva un dato!
        return codaMessaggi.take();
    }
        */

    //Guarda il primo elemento dalla lista senza rimuoverlo 
    public String peek(){
        return codaMessaggi.peek();
    }

    //Toglie il primo elemento e lo restituisce
    public String poll(){
        return codaMessaggi.poll();
    }

    public int getDimensione() {
        return codaMessaggi.size();
    }
}