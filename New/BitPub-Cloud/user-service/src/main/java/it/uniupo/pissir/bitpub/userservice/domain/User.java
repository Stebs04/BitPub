package it.uniupo.pissir.bitpub.userservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastLogin;
}
