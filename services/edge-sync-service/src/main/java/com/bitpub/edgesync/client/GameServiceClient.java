package com.bitpub.edgesync.client;

import com.bitpub.contracts.events.BaseSensorEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "game-service")
public interface GameServiceClient {

    @PostMapping("/api/v1/internal/sessions/events")
    void sendEvent(@RequestBody BaseSensorEvent event);
}
