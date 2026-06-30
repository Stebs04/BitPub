package it.uniupo.pissir.bitpub.statisticsservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "aggregate_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregateStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String entityId; // ID del giocatore, del locale, del gioco

    @Column(nullable = false)
    private String entityType; // PLAYER, LOCALE, GAME_TYPE

    @Column(nullable = false)
    private String metricName; // E.g., MATCHES_PLAYED, WIN_RATE, AVERAGE_SCORE

    @Column(nullable = false)
    private double metricValue;

    @Column(nullable = false)
    private Instant lastComputedAt;
}
