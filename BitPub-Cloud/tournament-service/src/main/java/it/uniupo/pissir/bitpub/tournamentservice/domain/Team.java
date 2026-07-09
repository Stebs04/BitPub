package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Squadra di torneo: qui i Team hanno un'anagrafica propria (name obbligatorio) e una lista
 * esplicita di membri (utenti) tramite la tabella di associazione team_members. A differenza del
 * match-service (dove i partecipanti alla singola partita sono MatchParticipant), il concetto di
 * "squadra" con identita' persistente esiste SOLO nei tornei.
 */
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
    private String name;

    // Torneo di appartenenza (loose FK per ID, coerente con il resto della piattaforma).
    private String tournamentId;

    // Membri della squadra (ID/username utenti) via tabella di associazione esplicita.
    @ElementCollection
    @CollectionTable(name = "team_members", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "member")
    private List<String> members;
}
