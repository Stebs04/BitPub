package com.bitpub.models;

public class StatisticheFreccette {

    // I nomi di queste variabili devono corrispondere ai nomi nel JSON inviato da Spring Boot
    private int totale180;
    private double mediaPuntiTorneo;

    // Metodi per leggere i dati
    public int getTotale180() {
        return totale180;
    }

    public double getMediaPuntiTorneo() {
        return mediaPuntiTorneo;
    }
}