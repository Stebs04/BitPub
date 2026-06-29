package com.bitpub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * GameEventDTO - Rappresentazione standard di un evento generato durante una sessione.
 *
 * Refactoring Note:
 * Sostituisce la vecchia BiliardoResource. È un DTO puro che segue le convenzioni 
 * di naming del progetto (snake_case per JSON) e garantisce il disaccoppiamento 
 * dai modelli HATEOAS e dalle Entity JPA.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameEventDTO {

    @JsonProperty("event_id")
    private Long eventId;

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("table_id")
    private String tableId;

    @JsonProperty("event_type")
    private String eventType; // es. BALL_IN_POCKET, SCORE_UPDATE, FOUL

    @JsonProperty("payload")
    private String payload; // Dettagli specifici dell'evento in formato stringa o JSON

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public GameEventDTO() {
    }

    public GameEventDTO(Long eventId, Long sessionId, String tableId, String eventType, String payload, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.tableId = tableId;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}