package com.bitpub.controllers;

import com.bitpub.services.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminEmergencyController - Endpoint per la gestione delle situazioni critiche.
 * * Refactoring Note:
 * Rimosso l'accoppiamento diretto con MqttAdminGateway.
 * Il controller ora inietta EmergencyService seguendo le best practices di Spring
 * e i principi architetturali del progetto, delegando la logica hardware al service.
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping("/api/admin/emergency")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmergencyController {

    private final EmergencyService emergencyService;

    @Autowired
    public AdminEmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    /**
     * Esegue lo sblocco forzato di un tavolo.
     * Riceve i parametri necessari nel corpo della richiesta per identificare il target.
     * * @param payload Map contenente 'localeId' e 'tavoloId'.
     * @return ResponseEntity con esito dell'operazione.
     */
    @PostMapping("/unlock")
    public ResponseEntity<String> emergencyUnlock(@RequestBody Map<String, Object> payload) {
        try {
            Long localeId = Long.valueOf(payload.get("localeId").toString());
            String tavoloId = payload.get("tavoloId").toString();

            // Delega al service l'esecuzione del comando hardware
            emergencyService.forceUnlockTable(localeId, tavoloId);

            return ResponseEntity.ok("Comando di sblocco inviato con successo.");
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Parametri 'localeId' o 'tavoloId' mancanti o non validi.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Errore durante l'invio del comando di emergenza.");
        }
    }
}