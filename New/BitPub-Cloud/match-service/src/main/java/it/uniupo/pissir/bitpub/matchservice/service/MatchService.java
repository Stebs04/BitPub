package it.uniupo.pissir.bitpub.matchservice.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;

public interface MatchService {
    MatchDto startMatch(StartMatchRequestDto request);
    MatchDto endMatch(String matchId);
    MatchDto getMatch(String matchId);
    void processSensorEvent(SensorEvent event);
}
