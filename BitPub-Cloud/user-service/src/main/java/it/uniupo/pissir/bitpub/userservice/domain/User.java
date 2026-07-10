package it.uniupo.pissir.bitpub.userservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Entità JPA che rappresenta un utente all'interno del sistema.
 * Mappa la tabella "users" nel database e definisce le proprietà fondamentali 
 * come credenziali, ruolo e associazioni specifiche.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash; // In un'app reale sarebbe l'hash, o gestita dall'auth-service

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role; // PLAYER, LOCALE_ADMIN, GAME_ADMIN, PLATFORM_ADMIN

    private String localeId; // Valorizzato solo per LOCALE_ADMIN: locale di appartenenza

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastLogin;
}
