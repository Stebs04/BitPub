package it.bitpub.edge.buffer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Buffer in memoria (Heap) dell'Edge Node per lo stoccaggio dei log.
 * Tutti i metodi critici sono blindati tramite 'synchronized' per prevenire
 * deadlock e race conditions tra i thread di ricezione MQTT.
 */
public class BufferEventi {

    // La struttura dati in memoria (Heap) scelta per la coda FIFO (First-In-First-Out)
    private Queue<String> codaMessaggi;

    public BufferEventi() {
        this.codaMessaggi = new LinkedList<>();
    }

    /**
     * Metodo PUSH (Scrittura): Aggiunge un nuovo evento alla coda.
     * Usato dal thread di ricezione MQTT (messageArrived) di Stefano.
     * Il modificatore 'synchronized' garantisce l'accesso esclusivo.
     */
    public synchronized void aggiungiEvento(String payloadJson) {
        codaMessaggi.add(payloadJson);
        System.out.println("[Buffer Blindato] Evento aggiunto. Elementi in coda: " + codaMessaggi.size());
    }

    /**
     * Metodo POP (Lettura/Estrazione): Preleva e rimuove l'evento più vecchio.
     * Verrà usato dall'Edge Node per inoltrare i dati al Cloud.
     * Anche questo metodo deve essere 'synchronized' per non leggere
     * mentre un altro thread sta scrivendo.
     */
    public synchronized String prelevaEvento() {
        // Se la coda è vuota, restituiamo null in sicurezza
        if (codaMessaggi.isEmpty()) {
            return null;
        }

        // .poll() restituisce l'elemento in testa alla coda e lo rimuove
        String evento = codaMessaggi.poll();
        return evento;
    }

    /**
     * Metodo per controllare lo stato della coda in sicurezza.
     */
    public synchronized int getDimensione() {
        return codaMessaggi.size();
    }
}