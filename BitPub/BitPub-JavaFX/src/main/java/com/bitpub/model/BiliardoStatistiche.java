package com.bitpub.model;

import java.util.List;

/**
 * Questa classe rappresenta i dati statistici in arrivo dal Cloud.
 * I nomi delle variabili devono corrispondere al JSON generato dal server.
 */
public class BiliardoStatistiche {
    private int serieMassimaPalle;
    private List<String> storicoPartite; // Contiene gli ID o i riassunti delle partite passate

    // Metodi "Getter" per leggere i dati
    public int getSerieMassimaPalle() {
        return serieMassimaPalle;
    }

    public List<String> getStoricoPartite() {
        return storicoPartite;
    }
}