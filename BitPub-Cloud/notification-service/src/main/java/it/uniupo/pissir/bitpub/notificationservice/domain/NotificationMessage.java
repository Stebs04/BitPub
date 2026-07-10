package it.uniupo.pissir.bitpub.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * Rappresenta l'entità di dominio per un singolo messaggio di notifica inviato a un utente del sistema.
 * La classe mappa direttamente la struttura dei dati salvata nella tabella "notifications" del database.
 * Utilizza la libreria Lombok per alleggerire il codice autogenerando metodi accessori e costruttori.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    // Identificatore primario autogenerato per garantire l'univocità di ogni singola notifica
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Riferimento all'utente destinatario della comunicazione, requisito fondamentale per il recapito
    @Column(nullable = false)
    private String recipientUserId;

    // Specifica la natura della notifica permettendo al client di gestirla graficamente
    @Column(nullable = false)
    private String type; // PUSH, TOURNAMENT_INVITE, MATCH_RESULT

    // Intestazione testuale che riassume il contenuto della comunicazione
    @Column(nullable = false)
    private String title;

    // Corpo principale del messaggio contenente le informazioni di dettaglio
    @Column(nullable = false)
    private String content;
    
    // Collegamento opzionale che consente all'utente di interagire direttamente con il contenuto notificato
    @Column(columnDefinition = "TEXT")
    private String actionUrl; // URL opzionale per la Call To Action

    // Stato di lettura che indica se l'utente ha già visualizzato il messaggio o meno
    private boolean read;

    // Marca temporale esatta del momento in cui la notifica è stata generata dal sistema
    @Column(nullable = false)
    private Instant createdAt;
}
