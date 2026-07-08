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
    private String type; // Es. "GOAL", "BALL_POCKETED", "MATCH_START"

    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private boolean isActuator; // True se è un attuatore (es. DISPLAY)

    @Column(nullable = false)
    @Builder.Default
    private int scoreIncrement = 1; // Punti guadagnati quando il sensore si attiva con successo

    @Column(nullable = false)
    @Builder.Default
    private double successProbability = 1.0; // 0.0-1.0: probabilità RNG che l'azione vada a segno

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;
}
