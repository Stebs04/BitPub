package com.bitpub.model;

import java.util.UUID;
import java.time.LocalDateTime;

public class Device {
    private UUID id;
    private String macAddress;
    private String status;
    private LocalDateTime createdAt;
    
    // We only need the core fields for UI list
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
