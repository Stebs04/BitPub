package com.bitpub.services;

import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.bitpub.network.SessionManager;

import java.util.concurrent.CompletableFuture;

/**
 * Service class handling Authentication operations.
 */
public class AuthService {

    private final RestClient restClient;

    public AuthService() {
        this.restClient = RestClient.getInstance();
    }

    /**
     * Authenticates the user asynchronously using HATEOAS discovery.
     */
    public CompletableFuture<Void> loginAsync(String username, String password) {
        String loginUrl = restClient.getRootUrl() + "/api/v1/auth/login";
        AuthRequest request = new AuthRequest(username, password);
        return restClient.postAsync(loginUrl, request, AuthResponse.class)
                .thenAccept(authResponse -> {
                    if (authResponse != null && authResponse.getToken() != null) {
                        SessionManager.getInstance().setJwtToken(authResponse.getToken());
                        SessionManager.getInstance().setUserRole(authResponse.getRole());
                        SessionManager.getInstance().setUsername(username);
                        // Estrai userId dal JWT (il claim è nella parte payload del token)
                        try {
                            String[] parts = authResponse.getToken().split("\\.");
                            if (parts.length >= 2) {
                                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                                com.google.gson.JsonObject claims = com.google.gson.JsonParser.parseString(payloadJson).getAsJsonObject();
                                if (claims.has("userId")) {
                                    SessionManager.getInstance().setUserId(
                                        java.util.UUID.fromString(claims.get("userId").getAsString()));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[AuthService] Impossibile estrarre userId dal JWT: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("Risposta del server non valida.");
                    }
                });
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
