package it.uniupo.pissir.bitpub.localeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocaleDto {
    private String id;
    private String name;
    private String address;
    private String adminId;
    private List<GameInstanceDto> games;
}
