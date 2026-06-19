package com.bitpub.game.service;

import com.bitpub.contracts.events.BaseSensorEvent;
import com.bitpub.contracts.events.MatchEndedEvent;
import com.bitpub.contracts.events.MatchStartedEvent;
import com.bitpub.contracts.events.ScoreEvent;
import com.bitpub.game.model.MatchSession;
import com.bitpub.game.repository.MatchSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LiveSessionService {

    private final MatchSessionRepository matchSessionRepository;

    public LiveSessionService(MatchSessionRepository matchSessionRepository) {
        this.matchSessionRepository = matchSessionRepository;
    }

    public void processEvent(BaseSensorEvent event) {
        String sessionId = event.getCorrelationId() != null ? event.getCorrelationId().toString() : event.getSource();
        
        Optional<MatchSession> optionalMatch = matchSessionRepository.findBySessionId(sessionId);

        if (event instanceof MatchStartedEvent) {
            if (optionalMatch.isEmpty()) {
                MatchSession newMatch = MatchSession.builder()
                        .sessionId(sessionId)
                        .status("ONGOING")
                        .score1(0)
                        .score2(0)
                        .startedAt(LocalDateTime.now())
                        .build();
                matchSessionRepository.save(newMatch);
            }
        } else if (event instanceof ScoreEvent scoreEvent) {
            MatchSession match = optionalMatch.orElseGet(() -> 
                MatchSession.builder()
                        .sessionId(sessionId)
                        .status("ONGOING")
                        .score1(0)
                        .score2(0)
                        .startedAt(LocalDateTime.now())
                        .build()
            );
            
            match.setScore1(scoreEvent.getScoreTeamA());
            match.setScore2(scoreEvent.getScoreTeamB());
            matchSessionRepository.save(match);
            
        } else if (event instanceof MatchEndedEvent endEvent) {
            if (optionalMatch.isPresent()) {
                MatchSession match = optionalMatch.get();
                match.setScore1(endEvent.getFinalScoreTeamA());
                match.setScore2(endEvent.getFinalScoreTeamB());
                match.setStatus("COMPLETED");
                match.setEndedAt(LocalDateTime.now());
                matchSessionRepository.save(match);
            }
        }
    }

    public MatchSession getActiveSession(String sessionId) {
        return matchSessionRepository.findBySessionId(sessionId).orElse(null);
    }
}
