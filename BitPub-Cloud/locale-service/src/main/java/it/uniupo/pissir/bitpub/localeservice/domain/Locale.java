package it.uniupo.pissir.bitpub.localeservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "locales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Locale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String adminId; // Riferimento all'ID in user-service

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "locale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameInstance> gameInstances;
}
