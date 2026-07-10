/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entità che traccia le voci di classifica, raggruppate per giocatore e tipologia di gioco.
 * Il nome del giocatore viene memorizzato come stringa piatta per ottimizzare i tempi di lettura.
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

    /** Nome in chiaro del giocatore o della squadra per una rapida consultazione. */
    @Column(nullable = false)
    private String playerName;

    /** Tipologia di gioco a cui si riferisce questa voce di classifica (es. biliardo, freccette). */
    @Column(nullable = false)
    private String gameTypeId;

    /** Identificativo del locale associato all'ultimo scontro disputato, fondamentale per le proiezioni geografiche dell'amministratore. */
    private String localeId;

    /** Booleano che indica se questa voce classifica i risultati di un'intera squadra piuttosto che di un singolo partecipante. */
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
