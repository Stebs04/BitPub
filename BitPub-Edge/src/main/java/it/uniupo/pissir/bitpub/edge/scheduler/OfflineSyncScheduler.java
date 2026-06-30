package it.uniupo.pissir.bitpub.edge.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
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
    private final ObjectMapper objectMapper;

    public OfflineSyncScheduler(BufferedEventRepository repository, EventForwardingService forwardingService) {
        this.repository = repository;
        this.forwardingService = forwardingService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Scheduled(fixedDelay = 10000) // runs every 10 seconds
    @Transactional
    public void syncOfflineEvents() {
        List<BufferedEvent> pendingEvents = repository.findAllByOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} offline events to sync.", pendingEvents.size());

        for (BufferedEvent bufferedEvent : pendingEvents) {
            try {
                SensorEvent event = objectMapper.readValue(bufferedEvent.getPayloadJson(), SensorEvent.class);
                boolean success = forwardingService.forwardToCloud(event);
                if (success) {
                    log.info("Successfully synced event {} to cloud. Deleting from local DB.", event.getEventId());
                    repository.delete(bufferedEvent);
                } else {
                    log.warn("Cloud still unreachable, aborting sync cycle.");
                    break; // Stop syncing this cycle to preserve order and not bombard a down server
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse buffered event payload. Deleting corrupted event {}", bufferedEvent.getId(), e);
                repository.delete(bufferedEvent); // Delete unparseable event to avoid poisoning the queue
            }
        }
    }
}
