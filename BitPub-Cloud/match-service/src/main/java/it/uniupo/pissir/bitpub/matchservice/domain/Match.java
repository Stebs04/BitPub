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

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorEventLog> events;
    
    // Campo JSONB o simile per il risultato finale (es. punteggi)
    @Column(columnDefinition = "TEXT")
    private String resultPayload; 
}
