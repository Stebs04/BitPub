package com.bitpub.services;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.events.SessionForceStoppedEvent;
import com.bitpub.events.SessionStartedEvent;
import com.bitpub.models.Utente;
import com.bitpub.models.AuditLogEntity;
import com.bitpub.models.GameSessionEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GameSessionService - Logica di business per la gestione delle sessioni.
 * * Refactoring Senior Note:
 * Le operazioni di scrittura nel DB e la creazione dei log di audit sono ora 
 * atomiche grazie alla gestione transazionale. I log vengono popolati con 
 * metadati corretti per evitare eccezioni di persistenza.
 */
@Service
public class GameSessionService {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<GameSessionDTO> getActiveSessions() {
        return gameSessionRepository.findAllByStatus("IN_PROGRESS").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Interrompe forzatamente una sessione aggiornando DB, Log e segnalando l'evento all'hardware.
     */
    @Transactional
    public GameSessionDTO forceStopSession(Long id) {
        // 1. Recuperiamo la sessione dal DB
        GameSessionEntity session = gameSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Sessione con ID " + id + " non trovata."));

        // 2. Controlliamo che la sessione sia attiva
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("La sessione selezionata non è in corso (Stato attuale: " + session.getStatus() + ").");
        }

        // --- DIFESA: Preveniamo errori se tableId dovesse essere null ---
        Integer tableId = session.getTableId();
        if (tableId == null) {
            tableId = 0; // Usiamo un valore fittizio per evitare crash
        }

        // 3. Aggiorniamo lo stato
        session.setStatus("FORCE_STOPPED");
        session.setEndTime(LocalDateTime.now());
        gameSessionRepository.save(session);

        // 4. Registrazione Audit Log
        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("WARN");
        log.setSource("Cloud-Admin-Controller");
        log.setAction("SESSION_FORCE_STOPPED");
        log.setMessage("L'amministratore ha forzato la chiusura della sessione #" + id + " sul tavolo " + tableId);
        
        // Se il database rifiuta il salvataggio per problemi di tabella, l'errore scaturirà qui
        auditLogRepository.save(log);

        // 5. Pubblicazione evento interno usando il tableId sicuro
        eventPublisher.publishEvent(new SessionForceStoppedEvent(this, tableId, id));

        return convertToDTO(session);
    }

    public Optional<GameSessionDTO> getSessionById(Long id) {
        return gameSessionRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional
    public GameSessionDTO startSession(String username, Integer tableId) {
        Utente user = utenteRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utente '" + username + "' non trovato."));
        
        Long userId = user.getId();

        Optional<GameSessionEntity> activeSession = gameSessionRepository.findByUserIdAndStatus(userId, "IN_PROGRESS");
        if (activeSession.isPresent()) {
            throw new IllegalStateException("Impossibile avviare: hai già una partita attiva (ID: " + activeSession.get().getId() + ").");
        }

        GameSessionEntity newSession = new GameSessionEntity();
        newSession.setGameType("FOOSBALL");
        newSession.setTableId(tableId);
        newSession.setUserId(userId);
        newSession.setStatus("IN_PROGRESS");
        newSession.setStartTime(LocalDateTime.now());

        newSession = gameSessionRepository.save(newSession);

        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("INFO");
        log.setSource("Cloud-Foosball-Service");
        log.setAction("SESSION_STARTED");
        log.setMessage("Nuova sessione avviata su tavolo " + tableId + " per utente " + username);
        auditLogRepository.save(log);

        eventPublisher.publishEvent(new SessionStartedEvent(this, tableId, newSession.getId()));

        return convertToDTO(newSession);
    }

    public Optional<GameSessionDTO> getCurrentSession(String username) {
        return utenteRepository.findByUsername(username)
                .flatMap(u -> gameSessionRepository.findByUserIdAndStatus(u.getId(), "IN_PROGRESS"))
                .map(this::convertToDTO);
    }

    private GameSessionDTO convertToDTO(GameSessionEntity entity) {
        GameSessionDTO dto = new GameSessionDTO();
        dto.setId(entity.getId());
        dto.setTavoloId(String.valueOf(entity.getTableId()));
        dto.setUtenteId(entity.getUserId());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setStatus(entity.getStatus());
        dto.setGameType(entity.getGameType());
        
        // Tentativo di conversione sicura di venueId (String) in localeId (Long)
        if (entity.getVenueId() != null) {
            try {
                dto.setLocaleId(Long.parseLong(entity.getVenueId()));
            } catch (NumberFormatException e) {
                // Se non è un numero (es. è un nome), lo lasciamo null nel DTO numerico
            }
        }
        
        return dto;
    }
}