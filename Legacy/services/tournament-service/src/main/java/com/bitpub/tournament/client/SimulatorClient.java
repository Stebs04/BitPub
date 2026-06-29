package com.bitpub.tournament.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

@FeignClient(name = "simulator-service")
public interface SimulatorClient {

    @PostMapping("/api/v1/simulators/simulate/{gameType}")
    Map<String, Object> simulateMatch(@PathVariable("gameType") String gameType);
}
