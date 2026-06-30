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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id", nullable = false)
    private GameType gameType;
}
