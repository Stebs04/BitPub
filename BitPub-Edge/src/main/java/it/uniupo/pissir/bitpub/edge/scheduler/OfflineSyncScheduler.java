package it.uniupo.pissir.bitpub.edge.scheduler;

import it.uniupo.pissir.bitpub.edge.model.BufferedEvent;
import it.uniupo.pissir.bitpub.edge.repository.BufferedEventRepository;
import it.uniupo.pissir.bitpub.edge.service.EventForwardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class OfflineSyncScheduler {

    private final BufferedEventRepository repository;
    private final EventForwardingService forwardingService;

    public OfflineSyncScheduler(BufferedEventRepository repository, EventForwardingService forwardingService) {
        this.repository = repository;
        this.forwardingService = forwardingService;
    }

    @Scheduled(fixedDelay = 10000) // runs every 10 seconds
    @Transactional
    public void syncOfflineEvents() {
        List<BufferedEvent> pendingEvents = repository.findAllByOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} offline commands to sync.", pendingEvents.size());

        for (BufferedEvent cmd : pendingEvents) {
            boolean success = forwardingService.forwardCommand(cmd);
            if (success) {
                // Cloud dedups on originalEventId, so replaying a command it already applied is safe.
                log.info("Synced command {} ({} {}). Deleting from local buffer.",
                        cmd.getOriginalEventId(), cmd.getHttpMethod(), cmd.getTargetEndpoint());
                repository.delete(cmd);
            } else {
                // Stop the cycle on first failure: preserves ordering and does not hammer a down cloud.
                log.warn("Cloud still unreachable, aborting sync cycle.");
                break;
            }
        }
    }
}
