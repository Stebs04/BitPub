package com.bitpub.dto;

import java.time.LocalDateTime;

public class EdgeStatusDTO {
    private String edgeId;
    private String status;
    private LocalDateTime lastSeen;

    public EdgeStatusDTO() {}

    public String getEdgeId() { return edgeId; }
    public void setEdgeId(String edgeId) { this.edgeId = edgeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
