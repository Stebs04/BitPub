package it.uniupo.pissir.bitpub.statisticsservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks per-player, per-game-type leaderboard entries.
 * playerName is a plain string (not a FK) for fast demo usage.
 */
@Entity
@Table(
    name = "leaderboard",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playerName", "gameTypeId"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Free-text player/team name entered in the simulator setup. */
    @Column(nullable = false)
    private String playerName;

    /** E.g. "biliardo", "calciobalilla", "freccette" */
    @Column(nullable = false)
    private String gameTypeId;

    /** Locale in cui e' stato registrato l'ultimo match, per il filtro leaderboard-per-locale del LOCALE_ADMIN. */
    private String localeId;

    /** true se l'entry rappresenta una squadra (partita a squadre), false se un singolo giocatore. */
    @Builder.Default
    @Column(columnDefinition = "boolean not null default false")
    private boolean teamBased = false;

    @Builder.Default
    private int wins = 0;

    @Builder.Default
    private int losses = 0;

    @Builder.Default
    private int totalPoints = 0;

    @Builder.Default
    private int matchesPlayed = 0;

    private Instant lastUpdated;
}
