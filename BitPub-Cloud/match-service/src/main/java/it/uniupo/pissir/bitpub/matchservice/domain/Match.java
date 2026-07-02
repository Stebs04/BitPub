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
    private String gameInstanceId; // Riferimento all'istanza fisica (da locale-service)

    // Locale di appartenenza della gameInstance, risolto da locale-service alla creazione del match.
    // Usato per limitare l'accesso dei LOCALE_ADMIN alle sole partite del proprio locale.
    private String localeId;

    @Column(nullable = false)
    private String gameTypeId; // Es. ID per "Calciobalilla"

    @Column(nullable = false)
    private String status; // CREATED, IN_PROGRESS, COMPLETED, CANCELLED

    private Instant startTime;
    private Instant endTime;

    // ID utente a cui spetta il turno corrente. Assegnato casualmente allo START del match
    // e aggiornato ad ogni azione di gioco.
    private String currentTurnUserId;

    // Biliardo: la spaccata iniziale assegna casualmente "Piene" (solid) e "Spezzate" (striped)
    // ai due team. breakDone segna se la spaccata e' gia' avvenuta.
    // columnDefinition con DEFAULT esplicito: senza, Hibernate genera "ADD COLUMN ... NOT NULL"
    // senza default, che Postgres rifiuta se la tabella "matches" contiene gia' righe.
    @Column(columnDefinition = "boolean not null default false")
    private boolean breakDone;
    private String solidTeamId;   // team a cui sono state assegnate le Piene
    private String stripedTeamId; // team a cui sono state assegnate le Spezzate

    // Freccette: conteggio dei tiri effettuati nel turno corrente (il turno passa dopo 3 tiri).
    @Column(columnDefinition = "integer not null default 0")
    private int throwsInTurn;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorEventLog> events;
    
    // Campo JSONB o simile per il risultato finale (es. punteggi)
    @Column(columnDefinition = "TEXT")
    private String resultPayload; 
}
