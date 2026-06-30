package it.uniupo.pissir.bitpub.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String recipientUserId;

    @Column(nullable = false)
    private String type; // PUSH, TOURNAMENT_INVITE, MATCH_RESULT

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String actionUrl; // URL opzionale per la Call To Action

    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;
}
