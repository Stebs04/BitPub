package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {
    private String id;
    private String gameInstanceId;
    private String localeId;
    private String gameTypeId;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private List<TeamResponseDto> teams;
    private String resultPayload;

    // Stato del gameplay a turni (esposto al frontend per abilitare/disabilitare i controlli).
    private String currentTurnUserId;
    private boolean breakDone;
    private String solidTeamId;
    private String stripedTeamId;
}
