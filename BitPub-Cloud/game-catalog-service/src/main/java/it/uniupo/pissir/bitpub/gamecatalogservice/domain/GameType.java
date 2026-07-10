/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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
    private String name; // Identificativo testuale della specialità

    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private String rulesEngineId; // Codice univoco che il motore di simulazione utilizza per caricare la corretta strategia di calcolo

    @Column(nullable = false)
    @Builder.Default
    private int winScoreTarget = 10; // Soglia punti da raggiungere per decretare la vittoria

    @OneToMany(mappedBy = "gameType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorDefinition> sensors;
}
