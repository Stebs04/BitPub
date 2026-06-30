package it.uniupo.pissir.bitpub.localeservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "game_instances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"locale_id", "local_instance_id"}) // L'identificativo deve essere univoco nel locale
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // ID globale per il cloud

    @Column(name = "local_instance_id", nullable = false)
    private String localInstanceId; // Es. "calciobalilla-1" (unico dentro il locale)

    @Column(nullable = false)
    private String gameTypeId; // Riferimento all'ID in game-catalog-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locale_id", nullable = false)
    private Locale locale;

    @Column(nullable = false)
    private Instant installedAt;
    
    private boolean active;
}
