package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tournament_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private String participantId; // ID dell'utente o della squadra, a seconda di "teamBased"

    @Column(nullable = false)
    private String participantName; // Nome giocatore (individuale) o nome squadra (a squadre)

    // true = iscrizione a squadre (participantName = nome squadra, i membri sono in "members").
    @Builder.Default
    @Column(columnDefinition = "boolean not null default false")
    private boolean team = false;

    // Membri della squadra (username/ID dei giocatori associati). Per l'individuale contiene il solo giocatore.
    @ElementCollection
    @CollectionTable(name = "tournament_registration_members", joinColumns = @JoinColumn(name = "registration_id"))
    @Column(name = "member")
    private List<String> members;

    @Column(nullable = false)
    private String localeId; // ID del locale da cui partecipano

    @Column(nullable = false)
    private Instant registeredAt;
}
