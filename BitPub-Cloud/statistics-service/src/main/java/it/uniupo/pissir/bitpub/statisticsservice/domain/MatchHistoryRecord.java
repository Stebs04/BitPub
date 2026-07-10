/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Rappresentazione immutabile dello storico di una partita una volta che questa è stata completata.
 * A differenza delle metriche aggregate che possono subire ricalcoli, questi record costituiscono 
 * l'effettiva fonte di verità inalterabile su cui basare l'intero calcolo statistico.
 * L'ID della partita viene inoltre impiegato per gestire in modo sicuro e idempotente la ricezione di eventi MQTT.
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
