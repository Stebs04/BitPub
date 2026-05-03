package com.bitpub.edge;

import java.util.concurrent.ConcurrentHashMap;

/**
 * FILE 18: GameTableStateManager
 * Gestione thread-safe dello stato dei tavoli simulati.
 */
public class GameTableStateManager {

    // tableId -> "FREE" / "OCCUPIED"
    private final ConcurrentHashMap<Integer, String> tableStates = new ConcurrentHashMap<>();

    public void setOccupied(int tableId) {
        tableStates.put(tableId, "OCCUPIED");
        System.out.println("[STATE MANAGER] Tavolo " + tableId + " impostato su OCCUPIED");
    }

    public void setFree(int tableId) {
        tableStates.put(tableId, "FREE");
        System.out.println("[STATE MANAGER] Tavolo " + tableId + " impostato su FREE");
    }

    public boolean isOccupied(int tableId) {
        return "OCCUPIED".equals(getStatus(tableId));
    }

    public String getStatus(int tableId) {
        return tableStates.getOrDefault(tableId, "FREE");
    }

    public void printAllStates() {
        System.out.println("=== STATO ATTUALE DEI TAVOLI ===");
        if (tableStates.isEmpty()) {
            System.out.println("Nessun tavolo registrato.");
        } else {
            tableStates.forEach((id, status) -> 
                System.out.println("Tavolo ID " + id + " -> " + status)
            );
        }
        System.out.println("================================");
    }
}
