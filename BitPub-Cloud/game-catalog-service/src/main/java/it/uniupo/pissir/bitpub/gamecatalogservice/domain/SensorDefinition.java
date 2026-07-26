/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sensor_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String type; // Classificazione della natura dell'evento catturato dal sensore

    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private boolean isActuator; // Determina se il componente ha capacità di output attive nel mondo fisico

    @Column(nullable = false, columnDefinition = "integer default 1 not null")
    @Builder.Default
    private int scoreIncrement = 1; // Valore numerico apportato al punteggio ad ogni rilevazione utile

    @Column(nullable = false, columnDefinition = "double precision default 1.0 not null")
    @Builder.Default
    private double successProbability = 1.0; // Fattore di tolleranza o tasso di conversione successo/fallimento dell'azione

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;
}
