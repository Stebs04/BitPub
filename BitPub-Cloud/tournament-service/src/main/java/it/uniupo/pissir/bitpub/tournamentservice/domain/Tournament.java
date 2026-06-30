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
    private String gameTypeId; // Es. ID di Calciobalilla

    @Column(nullable = false)
    private boolean teamBased; // Se le partite sono a squadre o individuali

    private Instant startDate;
    private Instant endDate;
    
    @Column(nullable = false)
    private String status; // UPCOMING, ACTIVE, COMPLETED
    
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentRegistration> registrations;
}
