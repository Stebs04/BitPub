package it.uniupo.pissir.bitpub.localeservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Entita' che rappresenta una specifica istanza di gioco installata all'interno di un locale.
 * Mappa la tabella 'game_instances' nel database.
 */

@Entity
@Table(name = "game_instances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"locale_id", "local_instance_id"}) // Garantisce che l'identificativo locale della macchina sia univoco per quel locale
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Identificativo globale per il sistema cloud

    @Column(name = "local_instance_id", nullable = false)
    private String localInstanceId; // Identificativo fisico della macchina, ad esempio "calciobalilla-1" (deve essere univoco all'interno del singolo locale)

    @Column(nullable = false)
    private String gameTypeId; // Riferimento all'identificativo del tipo di gioco gestito dal game-catalog-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locale_id", nullable = false)
    private Locale locale;

    @Column(nullable = false)
    private Instant installedAt;
    
    private boolean active;
}
