package com.bitpub.events;

import org.springframework.context.ApplicationEvent;

public class EdgeStatusUpdateEvent extends ApplicationEvent {
    private final String venueId;
    private final String status;

    public EdgeStatusUpdateEvent(Object source, String venueId, String status) {
        super(source);
        this.venueId = venueId;
        this.status = status;
    }

    public String getVenueId() {
        return venueId;
    }

    public String getStatus() {
        return status;
    }
}
