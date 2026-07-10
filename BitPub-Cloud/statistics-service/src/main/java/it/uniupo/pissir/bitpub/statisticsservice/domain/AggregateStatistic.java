/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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
    private String entityId; // Identificativo dell'entità target (es. ID del giocatore, del locale o del gioco)

    @Column(nullable = false)
    private String entityType; // Tipologia dell'entità coinvolta (es. PLAYER, LOCALE, GAME_TYPE)

    @Column(nullable = false)
    private String metricName; // Nome identificativo della metrica calcolata (es. MATCHES_PLAYED, WIN_RATE)

    @Column(nullable = false)
    private double metricValue;

    @Column(nullable = false)
    private Instant lastComputedAt;
}
