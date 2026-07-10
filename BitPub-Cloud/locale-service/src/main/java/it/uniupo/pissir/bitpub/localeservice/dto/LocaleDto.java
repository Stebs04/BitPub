package it.uniupo.pissir.bitpub.localeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Data Transfer Object per la rappresentazione dei dati relativi a un locale.
 * Contiene sia le informazioni anagrafiche sia la lista dei dispositivi associati.
 */
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
