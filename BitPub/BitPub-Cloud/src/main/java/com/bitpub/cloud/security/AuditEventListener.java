package com.bitpub.cloud.security;

import com.bitpub.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    public void processAuditLog(AuditApplicationEvent event) {
        auditLogRepository.save(event.getAuditLogEntity());
    }
}
