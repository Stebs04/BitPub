package com.bitpub;

import com.bitpub.simulators.SimFreccette;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        // 1. Prepariamo i dati del nostro bersaglio
        // Usiamo "127.0.0.1" (che significa localhost) perché per ora stiamo
        // facendo i test sul tuo stesso computer, dove poi girerà il nodo Edge.
        String idLocale = "pub_centrale";
        String idDispositivo = "freccette_A";
        String ipEdgeNodo = "127.0.0.1";

        // 2. "Costruiamo" il nostro simulatore usando la classe che hai creato prima
        SimFreccette mioBersaglio = new SimFreccette(idLocale, idDispositivo, ipEdgeNodo);

        // 3. Affidiamo il simulatore a un "Thread" (un processo parallelo)
        // Questo è il Multithreading: permette alle tue freccette di giocare
        // contemporaneamente al Biliardo di Luca e al Calciobalilla di Stefano!
        Thread threadFreccette = new Thread(mioBersaglio);

        // 4. Facciamo partire la partita!
        threadFreccette.start();

        System.out.println("Comando di avvio inviato al simulatore Freccette.");

        // --- SPAZIO PER GLI ALTRI COLLEGHI ---
        // Qui sotto, in futuro, Stefano e Luca aggiungeranno le righe per
        // avviare anche SimCalciobalilla e SimBiliardo.
    }
}