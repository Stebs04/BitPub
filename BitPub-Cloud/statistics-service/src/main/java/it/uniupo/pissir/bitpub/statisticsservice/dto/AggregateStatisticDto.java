/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateStatisticDto {
    private String id;
    private String entityId;
    private String entityType;
    private String metricName;
    private double metricValue;
    private Instant lastComputedAt;
}
