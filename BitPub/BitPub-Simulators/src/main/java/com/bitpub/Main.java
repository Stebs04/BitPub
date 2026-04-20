package com.bitpub;

import com.bitpub.SimFreccette;
import com.bitpub.SimCalciobalilla;
import com.bitpub.SimBiliardo;

/**
 * Entry point principale per l'ecosistema di simulazione IoT BitPub.
 * Questa classe orchestra l'inizializzazione e l'avvio concorrente dei simulatori
 * per Freccette, Calciobalilla e Biliardo, collegandoli all'Edge Gateway locale.
 *
 * @author Stefano Bellan 20054330 Timothy Giolito

 */
public class Main {

    /**
     * Metodo di avvio del sistema. Configura i parametri di rete e lancia i thread
     * dedicati per ogni dispositivo simulato.
     *
     * @param args Argomenti da riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        System.out.println("--- Avvio dei Simulatori IoT BitPub ---");

        // Configurazione centralizzata dei parametri di rete e localizzazione
        String idLocale = "pub_centrale";
        String ipEdgeNodo = "127.0.0.1"; // Indirizzo del broker MQTT locale (Edge)

        // 1. CONFIGURAZIONE SIMULATORE FRECCETTE
        String idFreccette = "freccette_A";
        SimFreccette mioBersaglio = new SimFreccette(idLocale, idFreccette, ipEdgeNodo);
        Thread threadFreccette = new Thread(mioBersaglio);

        // 2. CONFIGURAZIONE SIMULATORE CALCIOBALILLA
        String idCalciobalilla = "calciobalilla_1";
        SimCalciobalilla mioTavolo = new SimCalciobalilla(idLocale, idCalciobalilla, ipEdgeNodo);
        Thread threadCalciobalilla = new Thread(mioTavolo);

        // 3. CONFIGURAZIONE SIMULATORE BILIARDO
        String idBiliardo = "biliardo_1";
        SimBiliardo mioTavoloBiliardo = new SimBiliardo(idLocale, idBiliardo, ipEdgeNodo);
        Thread threadBiliardo = new Thread(mioTavoloBiliardo);

        /**
         * ESECUZIONE CONCORRENTE:
         * Ogni simulatore implementa l'interfaccia Runnable. L'avvio tramite Thread
         * permette ai dispositivi di generare eventi in parallelo senza bloccarsi a vicenda.
         */
        threadFreccette.start();
        threadCalciobalilla.start();
        threadBiliardo.start();

        // Log di conferma per l'operatore di sistema
        System.out.println("Sistema avviato: Freccette (" + idFreccette +
                "), Calciobalilla (" + idCalciobalilla +
                ") e Biliardo (" + idBiliardo + ") sono online.");
    }
}
