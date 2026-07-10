/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Rappresenta un torneo all'interno del sistema.
 * Gestisce tutte le informazioni principali della competizione, inclusi i locali partecipanti,
 * le iscrizioni e il tabellone degli scontri.
 */
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
    private String gameTypeId; // Identificativo del tipo di gioco (es. Calciobalilla). Ogni torneo è dedicato a una singola disciplina

    @Column(nullable = false)
    private boolean teamBased; // Flag che indica se il torneo prevede scontri a squadre o individuali

    // Lista dei locali associati al torneo, dai quali i giocatori possono effettuare l'iscrizione
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tournament_locales", joinColumns = @JoinColumn(name = "tournament_id"))
    @Column(name = "locale_id")
    private List<String> localeIds;

    private Instant startDate;
    private Instant endDate;

    @Column(nullable = false)
    private String status; // Stato attuale del torneo (es. UPCOMING, ACTIVE, COMPLETED)

    // Limite massimo di partecipanti al torneo, fondamentale per la generazione corretta del tabellone (solitamente potenze di 2)
    private Integer maxParticipants;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentRegistration> registrations;

    // Scontri del tabellone a eliminazione diretta. Vengono mantenuti sul database per lo storico post-torneo
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentMatch> bracketMatches;
}
