// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Rappresenta un partecipante in una partita (un giocatore o una squadra).
 * Si differenzia dalle entità Team di torneo, gestite dal tournament-service.
 * Questa entità serve unicamente per modellare l'assegnazione e i punteggi all'interno della singola partita.
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
    private String name; // Nome rappresentativo del partecipante (es. "Mario Rossi" per individuali o "RED"/"BLUE" per squadre)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // Relazione con i giocatori; al fine di garantire un basso accoppiamento, vengono memorizzati esclusivamente gli ID
    @ElementCollection
    @CollectionTable(name = "participant_players", joinColumns = @JoinColumn(name = "participant_id"))
    @Column(name = "user_id")
    private List<String> playerIds;

    private int score; // Punteggio corrente accumulato dal partecipante
}
