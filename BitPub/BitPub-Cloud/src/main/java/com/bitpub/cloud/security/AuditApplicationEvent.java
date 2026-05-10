package com.bitpub.cloud.security;

import com.bitpub.models.AuditLogEntity;
import org.springframework.context.ApplicationEvent;

public class AuditApplicationEvent extends ApplicationEvent {

    private final AuditLogEntity auditLogEntity;

    public AuditApplicationEvent(Object source, AuditLogEntity auditLogEntity) {
        super(source);
        this.auditLogEntity = auditLogEntity;
    }

    public AuditLogEntity getAuditLogEntity() {
        return auditLogEntity;
    }
}
