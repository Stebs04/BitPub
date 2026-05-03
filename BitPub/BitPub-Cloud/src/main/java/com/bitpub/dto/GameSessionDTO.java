package com.bitpub.dto;

import com.bitpub.repository.GameSessionEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object per esporre solo i campi sicuri e necessari della sessione.
 */
@Data
@NoArgsConstructor
public class GameSessionDTO {
    private Long id;
    private String gameType;
    private Integer tableId;
    private String status;
    private Integer scoreBlue;
    private Integer scoreRed;

    // Costruttore di mapping dall'entità JPA
    public GameSessionDTO(GameSessionEntity entity) {
        this.id = entity.getId();
        this.gameType = entity.getGameType();
        this.tableId = entity.getTableId();
        this.status = entity.getStatus();
        this.scoreBlue = entity.getScoreBlue();
        this.scoreRed = entity.getScoreRed();
    }
}
