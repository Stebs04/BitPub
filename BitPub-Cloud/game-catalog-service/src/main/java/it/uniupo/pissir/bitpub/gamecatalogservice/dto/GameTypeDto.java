package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameTypeDto {
    private String id;
    private String name;
    private String description;
    private List<SensorDefinitionDto> sensors;
}
