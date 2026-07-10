package it.uniupo.pissir.bitpub.localeservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Entita' che rappresenta un locale fisico nel sistema.
 * Mappa la tabella 'locales' nel database e definisce la relazione uno-a-molti con le istanze di gioco.
 */

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
    private String adminId; // Identificativo dell'utente amministratore del locale, in riferimento allo user-service

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "locale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameInstance> gameInstances;
}
