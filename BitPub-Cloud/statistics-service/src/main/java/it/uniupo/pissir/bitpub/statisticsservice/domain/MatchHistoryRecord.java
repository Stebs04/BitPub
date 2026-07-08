package it.uniupo.pissir.bitpub.statisticsservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Storico stabile di una partita conclusa: chi ha vinto/perso, punteggi e tipo di gioco.
 * Distinto dagli aggregati (Leaderboard / AggregateStatistic), che vengono azzerati e
 * ricostruiti dal backfill; questo storico e' la fonte di verita' per-partita.
 * matchId e' univoco e funge da chiave di idempotenza per l'ingest MQTT (riconsegne QoS1).
 */
@Entity
@Table(name = "match_history", uniqueConstraints = @UniqueConstraint(columnNames = "matchId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String matchId;

    private String gameTypeId;
    private String winnerName;
    private String loserName;
    private int winnerScore;
    private int loserScore;
    private boolean teamBased;

    private Instant timestamp;
}
