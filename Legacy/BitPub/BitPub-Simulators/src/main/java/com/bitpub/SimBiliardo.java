package com.bitpub;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Modello dei dati puro. Nessuna traccia di logica di rete o code qui!
 */
class EventoBiliardo {
    String tipoEvento;
    String giocatore;
    int pallaImbucata;
    long timestamp;

    public EventoBiliardo(String tipoEvento, String giocatore, int pallaImbucata) {
        this.tipoEvento = tipoEvento;
        this.giocatore = giocatore;
        this.pallaImbucata = pallaImbucata;
        this.timestamp = System.currentTimeMillis();
    }
}

/**
 * Il simulatore ora è un task (Runnable) pulito che riceve la sua "scatola" (coda) dall'esterno.
 */
public class SimBiliardo implements Runnable {

    private final String idLocale;
    private final String idTavolo;
    private final Random random;
    private final String[] giocatori = {"Luca", "Stefano", "Timothy"};

    // Ecco la nostra coda che viene iniettata dall'esterno (Main)
    private final BlockingQueue<Object> codaEventi;

    // Aggiungiamo la coda come parametro obbligatorio del costruttore
    public SimBiliardo(String idLocale, String idTavolo, BlockingQueue<Object> codaEventi) {
        this.idLocale = idLocale;
        this.idTavolo = idTavolo;
        this.codaEventi = codaEventi;
        this.random = new Random();
    }

    @Override
    public void run() {
        // 1. Logica del gioco pura
        String giocatoreCorrente = giocatori[random.nextInt(giocatori.length)];
        int palla = 1 + random.nextInt(15);

        // 2. Creazione dell'evento (Oggetto Java normale)
        EventoBiliardo evento = new EventoBiliardo("IMBUCATA", giocatoreCorrente, palla);

        // 3. Invio dei dati alla coda locale iniettata, senza usare classi statiche come EventBus!
        codaEventi.offer(evento);

        System.out.println("[SimBiliardo " + idTavolo + "] " + giocatoreCorrente + " ha imbucato la palla " + palla + ". (Dato inserito nella coda)");
    }
}