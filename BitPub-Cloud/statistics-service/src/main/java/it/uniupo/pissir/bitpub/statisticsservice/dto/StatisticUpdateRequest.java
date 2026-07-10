/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticUpdateRequest {
    private String entityId;
    private String entityType;
    private String metricName;
    private double deltaValue;  // Rappresenta l'incremento numerico o il valore assoluto della metrica da aggiornare
    private boolean isAbsolute; // Flag per decidere se sovrascrivere l'attuale metrica (true) o sommare il deltaValue (false)
}
