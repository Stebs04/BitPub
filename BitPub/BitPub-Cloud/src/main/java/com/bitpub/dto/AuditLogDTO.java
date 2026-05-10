package com.bitpub.dto;

import java.time.LocalDateTime;

public class AuditLogDTO {
    private Long id;
    private String source;
    private String action;
    private String level;
    private String message;
    private LocalDateTime timestamp;

    public AuditLogDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
