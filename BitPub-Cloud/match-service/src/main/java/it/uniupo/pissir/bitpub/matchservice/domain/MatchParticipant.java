package it.uniupo.pissir.bitpub.matchservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Uno slot di una partita libera (lato/giocatore), con nome, membri e punteggio corrente.
 * NON e' una squadra di torneo: i veri Team con anagrafica vivono nel tournament-service.
 * Qui e' solo il partecipante alla singola partita.
 */
@Entity
@Table(name = "match_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name; // Es. "Mario Rossi" (individuale) o "RED"/"BLUE"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // Relazione coi giocatori, memorizziamo solo gli ID
    @ElementCollection
    @CollectionTable(name = "participant_players", joinColumns = @JoinColumn(name = "participant_id"))
    @Column(name = "user_id")
    private List<String> playerIds;

    private int score; // Punteggio corrente
}
