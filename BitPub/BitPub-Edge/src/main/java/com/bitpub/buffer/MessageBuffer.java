package com.bitpub.buffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Buffer basato su LinkedBlockingQueue.
 * Zero 'synchronized': la sicurezza concorrente è gestita nativamente da Java.
 */
public class MessageBuffer {

    // Utilizziamo BlockingQueue al posto di Queue
    private final BlockingQueue<String> codaMessaggi;

    public MessageBuffer() {
        // LinkedBlockingQueue non ha limiti di capienza (a meno che non venga specificato)
        this.codaMessaggi = new LinkedBlockingQueue<>();
    }

    // Usato da Stefano (Producer)
    public void push(String messaggio) {
        codaMessaggi.offer(messaggio); // Inserimento thread-safe
        System.out.println("[EDGE BUFFER] Log aggiunto. In coda: " + codaMessaggi.size());
    }

    // Usato da te, Timothy (Consumer)
    // Usiamo take() che lancia InterruptedException se il thread viene fermato mentre aspetta
    public String take() throws InterruptedException {
        // take() mette in pausa il thread se la coda è vuota, finché non arriva un dato!
        return codaMessaggi.take();
    }

    public int getDimensione() {
        return codaMessaggi.size();
    }
}