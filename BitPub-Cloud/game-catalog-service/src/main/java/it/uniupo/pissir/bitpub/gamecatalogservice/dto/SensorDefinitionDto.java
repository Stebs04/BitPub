package it.uniupo.pissir.bitpub.gamecatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDefinitionDto {
    private String id;
    private String type; // es. DIGITAL, ANALOG, RFID
    private String description;
    private boolean isActuator;
}
