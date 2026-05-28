package com.bitpub.domain;

import java.time.LocalDateTime;

/**
 * Modello per lo stato di un Edge Node.
 * Utilizzato per il monitoraggio della rete nella dashboard amministrativa.
 */
public class EdgeStatus {
    private String venueName;
    private String status;
    private LocalDateTime lastSeen;

    public EdgeStatus() {}

    public EdgeStatus(String venueName, String status, LocalDateTime lastSeen) {
        this.venueName = venueName;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
}


