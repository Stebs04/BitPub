package com.bitpub.buffer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Buffer in memoria (Heap) per lo stoccaggio temporaneo dei messaggi MQTT.
 * Utilizza metodi sincronizzati per gestire in sicurezza la concorrenza tra i thread.
 */
public class MessageBuffer {

    // La nostra struttura dati che funge da coda (First-In, First-Out)
    private final Queue<String> codaMessaggi;

    // Costruttore: inizializziamo la coda vuota quando l'Edge si accende
    public MessageBuffer() {
        this.codaMessaggi = new LinkedList<>();
    }

    /**
     * PUSH: Aggiunge un nuovo messaggio alla coda.
     * La parola 'synchronized' impedisce a due simulatori di scrivere nello stesso istante.
     * * @param messaggio Il payload JSON ricevuto dal broker MQTT locale.
     */
    public synchronized void push(String messaggio) {
        codaMessaggi.add(messaggio);
        System.out.println("[EDGE BUFFER] Nuovo log immagazzinato. Messaggi in coda: " + codaMessaggi.size());
    }

    /**
     * POP: Preleva e rimuove il messaggio più vecchio dalla coda.
     * Usato dal sistema quando è pronto a inviare i dati al Cloud.
     * La parola 'synchronized' evita che il Cloud legga dati parziali o acceda mentre qualcuno sta scrivendo.
     * * @return Il messaggio JSON, oppure null se la coda è vuota.
     */
    public synchronized String pop() {
        // Se non ci sono messaggi, ritorniamo null
        if (codaMessaggi.isEmpty()) {
            return null;
        }

        // .poll() legge il primo elemento della coda e lo RUMUOVE dal buffer
        return codaMessaggi.poll();
    }

    /**
     * Metodo di supporto per controllare quanti messaggi sono attualmente salvati.
     */
    public synchronized int getDimensione() {
        return codaMessaggi.size();
    }
}