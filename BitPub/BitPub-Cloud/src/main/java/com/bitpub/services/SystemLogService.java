package com.bitpub.services;

import com.bitpub.dto.AuditLogDTO;
import com.bitpub.dto.EdgeStatusDTO;
import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.cloud.repository.EdgeStatusEntity;
import com.bitpub.repository.EdgeStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemLogService {

    @Autowired
    private AuditLogRepository logRepository;

    @Autowired
    private EdgeStatusRepository edgeStatusRepository;

    public List<AuditLogDTO> getLogs(String level) {
        List<AuditLogEntity> entities;
        if (level != null && !level.isEmpty() && !"ALL".equals(level)) {
            entities = logRepository.findByLevel(level);
        } else {
            entities = logRepository.findAll();
        }
        return entities.stream().map(this::convertAuditToDTO).collect(Collectors.toList());
    }

    public List<EdgeStatusDTO> getNetworkStatus() {
        return edgeStatusRepository.findAll().stream()
                .map(this::convertEdgeToDTO)
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

    private EdgeStatusDTO convertEdgeToDTO(EdgeStatusEntity entity) {
        EdgeStatusDTO dto = new EdgeStatusDTO();
        dto.setEdgeId(entity.getEdgeId());
        dto.setStatus(entity.getStatus());
        dto.setLastSeen(entity.getLastSeen());
        return dto;
    }
}
