package it.uniupo.pissir.bitpub.localeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameInstanceDto {
    private String id;
    private String localInstanceId;
    private String gameTypeId;
    private String localeId;
    private Instant installedAt;
    private boolean active;
}
