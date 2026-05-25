package com.bitpub.model;

import java.util.UUID;

public class TeamModel {
    private UUID id;
    private UUID tournamentId;
    private String name;
    private int seed;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTournamentId() { return tournamentId; }
    public void setTournamentId(UUID tournamentId) { this.tournamentId = tournamentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSeed() { return seed; }
    public void setSeed(int seed) { this.seed = seed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return name;
    }
}
