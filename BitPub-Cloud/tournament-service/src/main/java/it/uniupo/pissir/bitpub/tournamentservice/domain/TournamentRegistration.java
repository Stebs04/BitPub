package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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
    private String participantName; // Nome denormalizzato per ricerche rapide

    @Column(nullable = false)
    private String localeId; // ID del locale da cui partecipano

    @Column(nullable = false)
    private Instant registeredAt;
}
