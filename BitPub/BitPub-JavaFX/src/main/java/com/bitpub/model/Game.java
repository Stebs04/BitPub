package com.bitpub.model;

import java.util.UUID;

public class Game {
    private UUID id;
    private String name;
    private String description;
    private String genre;
    private boolean active;
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
