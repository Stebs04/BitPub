/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Modello per gestire l'iscrizione di un partecipante (singolo giocatore o squadra) a un torneo.
 * Mantiene le informazioni temporali, il locale di riferimento e il dettaglio dei membri in caso di competizione a squadre.
 */
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
    private String participantId; // Identificativo univoco del giocatore singolo o della squadra, in base al tipo di torneo

    @Column(nullable = false)
    private String participantName; // Nome visualizzato nel tabellone (nome dell'utente o della squadra)

    // Indica se l'iscrizione fa riferimento a una squadra. In caso affermativo, la lista dei giocatori viene popolata in "members"
    @Builder.Default
    @Column(columnDefinition = "boolean not null default false")
    private boolean team = false;

    // Elenco degli username o ID associati all'iscrizione. Per i tornei individuali conterra' solo l'utente stesso
    @ElementCollection
    @CollectionTable(name = "tournament_registration_members", joinColumns = @JoinColumn(name = "registration_id"))
    @Column(name = "member")
    private List<String> members;

    // Riferimento all'entita' Team, popolato unicamente se si tratta di un torneo a squadre.
    // Questo collegamento permette di risalire all'anagrafica completa della squadra al momento opportuno.
    private String teamId;

    @Column(nullable = false)
    private String localeId; // Identificativo del locale presso il quale e' stata finalizzata l'iscrizione

    @Column(nullable = false)
    private Instant registeredAt;
}
