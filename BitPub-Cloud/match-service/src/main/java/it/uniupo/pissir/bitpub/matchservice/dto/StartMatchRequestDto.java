package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartMatchRequestDto {
    private String gameInstanceId;
    private String gameTypeId;
    private List<TeamDto> teams;
}
