package com.bitpub.dto;

import lombok.Data;

/**
 * Oggetto di trasferimento dati per le statistiche aggregate di un locale.
 */
@Data
public class StatisticheLocaleDTO {
    private Long localeId;
    private int partiteTotali;
    private double percentualeVittorieRossi;
    private double percentualeVittorieBlu;
    private double durataMediaMinuti;
    private int totaleRullate;
}
