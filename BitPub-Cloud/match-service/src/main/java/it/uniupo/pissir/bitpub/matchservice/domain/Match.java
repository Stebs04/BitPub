// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String gameInstanceId; // Riferimento all'istanza fisica fornita dal servizio locale (locale-service)

    // Identificativo del locale a cui appartiene l'istanza di gioco, risolto tramite locale-service
    // al momento della creazione della partita. Viene impiegato per garantire che gli amministratori di locale
    // (LOCALE_ADMIN) possano accedere esclusivamente alle partite del proprio locale.
    private String localeId;

    @Column(nullable = false)
    private String gameTypeId; // Identificativo della tipologia di gioco (es. "Calciobalilla", "Biliardo")

    @Column(nullable = false)
    private String status; // Stato della partita: CREATED, IN_PROGRESS, COMPLETED, CANCELLED

    // Distingue tra partita individuale (un giocatore per squadra) e a squadre.
    // L'impostazione deriva dal numero di membri (playerIds.size() > 1) e viene persistita
    // per renderla esplicita a livello di entità e per propagarla correttamente allo statistics-service.
    // Nelle partite a squadre, il nome del team viene registrato, a differenza dei punteggi individuali.
    @Column(columnDefinition = "boolean not null default false")
    private boolean teamBased;

    private Instant startTime;
    private Instant endTime;

    // Identificativo dell'utente a cui spetta il turno corrente. L'assegnazione avviene casualmente
    // all'inizio della partita e viene aggiornata in base alle azioni di gioco registrate.
    private String currentTurnUserId;

    // Configurazione specifica per il biliardo: la prima spaccata assegna in modo casuale le palline
    // "Piene" (solid) e "Spezzate" (striped) ai due team. Il flag "breakDone" indica l'avvenuta spaccata.
    // Utilizziamo un columnDefinition con "default false" esplicito per evitare problemi di retrocompatibilità
    // con record esistenti durante la generazione del database tramite Hibernate in Postgres.
    @Column(columnDefinition = "boolean not null default false")
    private boolean breakDone;
    private String solidTeamId;   // Identificativo della squadra associata alle palline Piene
    private String stripedTeamId; // Identificativo della squadra associata alle palline Spezzate

    // Configurazione specifica per le freccette: contatore dei tiri effettuati durante il turno corrente.
    // Il turno viene ceduto al raggiungimento del terzo tiro.
    @Column(columnDefinition = "integer not null default 0")
    private int throwsInTurn;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchParticipant> teams;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorEventLog> events;
    
    // Memorizza il risultato finale della partita sotto forma di stringa JSON (ad es. per i punteggi finali)
    @Column(columnDefinition = "TEXT")
    private String resultPayload;

    // Quando valorizzato, indica che questa partita è associata a un incontro del torneo (bracket match).
    // È "nullable" in quanto le partite libere (casual) non appartengono ad alcun torneo.
    // Questo campo è essenziale per la comunicazione dei vincitori e delle statistiche verso il tournament-service.
    private String tournamentMatchId;
}
