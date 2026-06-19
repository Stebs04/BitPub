package com.bitpub.game.repository;

import com.bitpub.game.model.MatchSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchSessionRepository extends JpaRepository<MatchSession, UUID>, JpaSpecificationExecutor<MatchSession> {
    List<MatchSession> findByDeviceIdAndStatus(UUID deviceId, String status);
    java.util.Optional<MatchSession> findBySessionId(String sessionId);
}
