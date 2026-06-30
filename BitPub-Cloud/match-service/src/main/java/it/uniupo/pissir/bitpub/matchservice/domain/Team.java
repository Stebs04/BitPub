package it.uniupo.pissir.bitpub.matchservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name; // Es. "Squadra A" o "Mario Rossi" se individuale

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // Relazione coi giocatori, memorizziamo solo gli ID
    @ElementCollection
    @CollectionTable(name = "team_players", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "user_id")
    private List<String> playerIds;
    
    private int score; // Punteggio corrente
}
