package com.bitpub.services;

import com.bitpub.dto.AuditLogDTO;
import com.bitpub.models.EdgeStatus;
import com.bitpub.models.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.models.EdgeStatusEntity;
import com.bitpub.repository.EdgeStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import com.bitpub.events.EdgeStatusUpdateEvent;

@Service
public class SystemLogService {

    @Autowired
    private AuditLogRepository logRepository;

    @Autowired
    private EdgeStatusRepository edgeStatusRepository;

    @Async("mqttDbTaskExecutor")
    @EventListener
    public void handleEdgeStatusUpdate(EdgeStatusUpdateEvent event) {
        EdgeStatusEntity entity = edgeStatusRepository.findById(event.getVenueId())
            .orElse(new EdgeStatusEntity());
        entity.setVenueId(event.getVenueId());
        entity.setStatus(event.getStatus());
        entity.setLastSeen(LocalDateTime.now());
        edgeStatusRepository.save(entity);
    }

    public List<AuditLogDTO> getLogs(String level) {
        List<AuditLogEntity> entities;
        if (level != null && !level.isEmpty() && !"ALL".equals(level)) {
            entities = logRepository.findByLevel(level);
        } else {
            entities = logRepository.findAll();
        }
        return entities.stream().map(this::convertAuditToDTO).collect(Collectors.toList());
    }

    public List<EdgeStatus> getNetworkStatus() {
        return edgeStatusRepository.findAll().stream()
                .map(this::convertEdgeToModel)
                .collect(Collectors.toList());
    }

    private AuditLogDTO convertAuditToDTO(AuditLogEntity entity) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(entity.getId());
        dto.setSource(entity.getSource());
        dto.setAction(entity.getAction());
        dto.setLevel(entity.getLevel());
        dto.setMessage(entity.getMessage());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }

    private EdgeStatus convertEdgeToModel(EdgeStatusEntity entity) {
        EdgeStatus model = new EdgeStatus();
        model.setVenueName(entity.getVenueId());
        model.setStatus(entity.getStatus());
        model.setLastSeen(entity.getLastSeen());
        return model;
    }
}
