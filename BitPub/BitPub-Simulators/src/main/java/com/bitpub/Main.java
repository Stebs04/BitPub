package com.bitpub;

import com.bitpub.SimFreccette;
import com.bitpub.SimCalciobalilla;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        // Configurazione comune per la rete locale
        String idLocale = "pub_centrale";
        String ipEdgeNodo = "127.0.0.1";

        // CONFIGURAZIONE FRECCETTE (Timothy) ---
        String idFreccette = "freccette_A";
        SimFreccette mioBersaglio = new SimFreccette(idLocale, idFreccette, ipEdgeNodo);
        Thread threadFreccette = new Thread(mioBersaglio);

        // CONFIGURAZIONE CALCIOBALILLA (Stefano) ---
        String idCalciobalilla = "calciobalilla_1";
        SimCalciobalilla mioTavolo = new SimCalciobalilla(idLocale, idCalciobalilla, ipEdgeNodo);
        Thread threadCalciobalilla = new Thread(mioTavolo);

        // AVVIO MULTITHREADING ---
        // Avviamo i motori in parallelo.
        // start() crea un nuovo flusso di esecuzione per ogni simulatore.
        threadFreccette.start();
        threadCalciobalilla.start();

        System.out.println("Sistema avviato: Freccette (" + idFreccette + ") e Calciobalilla (" + idCalciobalilla + ") sono online.");
    }
}