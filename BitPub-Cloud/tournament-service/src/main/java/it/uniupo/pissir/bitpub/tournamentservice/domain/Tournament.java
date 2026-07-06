package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String gameTypeId; // Es. ID di Calciobalilla (un torneo = un solo tipo di gioco)

    @Column(nullable = false)
    private boolean teamBased; // Se le partite sono a squadre o individuali

    // Insieme dei locali coinvolti nel torneo. I giocatori si iscrivono da uno di questi locali.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tournament_locales", joinColumns = @JoinColumn(name = "tournament_id"))
    @Column(name = "locale_id")
    private List<String> localeIds;

    private Instant startDate;
    private Instant endDate;

    @Column(nullable = false)
    private String status; // UPCOMING, ACTIVE, COMPLETED

    // Numero massimo di partecipanti; il tabellone si genera al raggiungimento. Potenza di 2 (4/8/16).
    private Integer maxParticipants;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentRegistration> registrations;

    // Tabellone a eliminazione diretta (persistente: resta consultabile a torneo COMPLETED).
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentMatch> bracketMatches;
}
