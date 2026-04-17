package com.bitpub;

import com.bitpub.SimFreccette;
import com.bitpub.SimCalciobalilla;
import com.bitpub.SimBiliardo; // Aggiunto per importare il tuo simulatore

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

        // CONFIGURAZIONE BILIARDO (Luca) ---
        String idBiliardo = "biliardo_1";
        SimBiliardo mioTavoloBiliardo = new SimBiliardo(idLocale, idBiliardo, ipEdgeNodo);
        Thread threadBiliardo = new Thread(mioTavoloBiliardo);

        // AVVIO MULTITHREADING ---
        // Avviamo i motori in parallelo.
        // start() crea un nuovo flusso di esecuzione per ogni simulatore.
        threadFreccette.start();
        threadCalciobalilla.start();
        threadBiliardo.start(); // Aggiunto l'avvio del tuo thread!

        System.out.println("Sistema avviato: Freccette (" + idFreccette + "), Calciobalilla (" + idCalciobalilla + ") e Biliardo (" + idBiliardo + ") sono online.");
    }
}