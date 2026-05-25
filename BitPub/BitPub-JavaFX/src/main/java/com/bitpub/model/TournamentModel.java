package com.bitpub.model;

import java.util.List;
import java.util.UUID;

public class TournamentModel {
    private UUID id;
    private String name;
    private UUID gameId;
    private String format;
    private String status;
    private int maxParticipants;
    private int teamSize;
    private List<String> locationIds;
    private String startDate;
    private String endDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public List<String> getLocationIds() { return locationIds; }
    public void setLocationIds(List<String> locationIds) { this.locationIds = locationIds; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    @Override
    public String toString() {
        return name + " (" + status + ")";
    }
}
