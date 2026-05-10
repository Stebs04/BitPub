package com.bitpub.events;

import org.springframework.context.ApplicationEvent;

public class SessionForceStoppedEvent extends ApplicationEvent {
    private final Integer tableId;
    private final Long sessionId;

    public SessionForceStoppedEvent(Object source, Integer tableId, Long sessionId) {
        super(source);
        this.tableId = tableId;
        this.sessionId = sessionId;
    }

    public Integer getTableId() {
        return tableId;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
