package com.bitpub.services;

import com.bitpub.dto.GameSessionDTO;
import com.bitpub.events.SessionForceStoppedEvent;
import com.bitpub.events.SessionStartedEvent;
import com.bitpub.models.Utente;
import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.repository.GameSessionEntity;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                .map(GameSessionDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public GameSessionDTO forceStopSession(Long id) {
        GameSessionEntity session = gameSessionRepository.findById(id).orElse(null);
        if (session == null || !"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("Sessione non trovata o non in corso.");
        }

        session.setStatus("FORCE_STOPPED");
        session.setFinishedAt(LocalDateTime.now());
        gameSessionRepository.save(session);

        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("WARN");
        log.setSource("Cloud-Admin");
        log.setAction("SESSION_FORCE_STOPPED");
        log.setMessage("Sessione " + id + " interrotta forzatamente da Admin");
        auditLogRepository.save(log);

        // Pubblica l'evento locale
        eventPublisher.publishEvent(new SessionForceStoppedEvent(this, session.getTableId(), id));

        return new GameSessionDTO(session);
    }

    @Transactional
    public GameSessionDTO startSession(String username, Integer tableId) {
        Optional<Utente> userOpt = utenteRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Utente non trovato.");
        }
        Long userId = userOpt.get().getId();

        Optional<GameSessionEntity> activeSession = gameSessionRepository.findByUserIdAndStatus(userId, "IN_PROGRESS");
        if (activeSession.isPresent()) {
            throw new IllegalStateException("Hai già una partita in corso (ID: " + activeSession.get().getId() + "). Termina quella prima di iniziarne una nuova.");
        }

        BigDecimal fixedCost = new BigDecimal("1.00");

        GameSessionEntity newSession = GameSessionEntity.builder()
                .gameType("FOOSBALL")
                .tableId(tableId)
                .userId(userId)
                .status("IN_PROGRESS")
                .scoreBlue(0)
                .scoreRed(0)
                .costDeducted(fixedCost)
                .build();

        newSession = gameSessionRepository.save(newSession);

        AuditLogEntity log = new AuditLogEntity();
        log.setLevel("INFO");
        log.setSource("Cloud-Foosball");
        log.setAction("SESSION_STARTED");
        log.setMessage("Partita avviata su tavolo " + tableId + " da utente ID " + userId);
        auditLogRepository.save(log);

        // Pubblica evento locale per MQTT
        eventPublisher.publishEvent(new SessionStartedEvent(this, tableId, newSession.getId()));

        return new GameSessionDTO(newSession);
    }

    public Optional<GameSessionDTO> getCurrentSession(String username) {
        Optional<Utente> userOpt = utenteRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        Long userId = userOpt.get().getId();

        return gameSessionRepository.findByUserIdAndStatus(userId, "IN_PROGRESS")
                .map(GameSessionDTO::new);
    }
}
