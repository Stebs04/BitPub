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
    private double deltaValue; // Or absolute value if it's a replacement, but delta is common (e.g. +1 match)
    private boolean isAbsolute; // if true, metricValue = deltaValue. if false, metricValue += deltaValue
}
