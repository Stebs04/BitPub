package it.uniupo.pissir.bitpub.gamecatalogservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "game_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name; // Es. "Calciobalilla", "Freccette", "Biliardo"

    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private String rulesEngineId; // Identificativo della logica (Strategy) da applicare nel match-service

    @Column(nullable = false)
    @Builder.Default
    private int winScoreTarget = 10; // Punti necessari per vincere. Letto dinamicamente dal match-service.

    @OneToMany(mappedBy = "gameType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorDefinition> sensors;
}
