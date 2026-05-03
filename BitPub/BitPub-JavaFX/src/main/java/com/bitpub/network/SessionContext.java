package com.bitpub.network;

/**
 * Classe di contesto globale per mantenere lo stato della sessione attiva nel frontend.
 * Contiene unicamente variabili e metodi statici in modo da poter essere richiamata
 * da qualsiasi controller JavaFX senza passaggi di riferimento complessi.
 */
public class SessionContext {

    private static String jwtToken;
    private static Long currentSessionId;
    private static String currentSessionStatusUrl;
    private static boolean isAdmin;

    // Costruttore privato per impedire l'istanziazione della classe
    private SessionContext() {
    }

    // --- GETTERS & SETTERS ---

    public static String getJwtToken() {
        return jwtToken;
    }

    public static void setJwtToken(String jwtToken) {
        SessionContext.jwtToken = jwtToken;
    }

    public static Long getCurrentSessionId() {
        return currentSessionId;
    }

    public static void setCurrentSessionId(Long currentSessionId) {
        SessionContext.currentSessionId = currentSessionId;
    }

    public static String getCurrentSessionStatusUrl() {
        return currentSessionStatusUrl;
    }

    public static void setCurrentSessionStatusUrl(String currentSessionStatusUrl) {
        SessionContext.currentSessionStatusUrl = currentSessionStatusUrl;
    }

    public static boolean isAdmin() {
        return isAdmin;
    }

    public static void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    /**
     * Resetta tutti i dati salvati in memoria.
     * Da invocare tassativamente durante la fase di Logout dell'utente
     * per evitare la persistenza indesiderata dei permessi o del JWT.
     */
    public static void clearAll() {
        jwtToken = null;
        currentSessionId = null;
        currentSessionStatusUrl = null;
        isAdmin = false;
    }
}
