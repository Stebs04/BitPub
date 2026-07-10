/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Entità che rappresenta una squadra all'interno di un torneo.
 * A differenza di altre parti del sistema (come il match-service, dove i partecipanti sono effimeri),
 * qui le squadre possiedono un'anagrafica vera e propria con un nome obbligatorio e una lista di membri
 * gestita tramite una tabella di associazione dedicata. Questo permette di mantenere l'identità della squadra
 * per tutta la durata della competizione.
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

    // Riferimento al torneo a cui è iscritta la squadra (chiave esterna debole sull'ID)
    private String tournamentId;

    // Lista degli username o ID degli utenti che compongono la squadra
    @ElementCollection
    @CollectionTable(name = "team_members", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "member")
    private List<String> members;
}
