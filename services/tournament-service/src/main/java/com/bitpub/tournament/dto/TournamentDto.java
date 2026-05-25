package com.bitpub.tournament.dto;

import com.bitpub.tournament.model.TournamentFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDto {
    private UUID id;
    private String name;
    private UUID gameId;
    private TournamentFormat format;
    private String status;
    private int maxParticipants;
    private int teamSize;
    private List<String> locationIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
