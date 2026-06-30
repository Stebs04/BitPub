package it.uniupo.pissir.bitpub.matchservice.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import java.util.List;

public interface MatchService {
    MatchDto startMatch(StartMatchRequestDto request);
    MatchDto endMatch(String matchId);
    MatchDto getMatch(String matchId);
    List<MatchDto> getActiveMatches();
    void processSensorEvent(SensorEvent event);
}
