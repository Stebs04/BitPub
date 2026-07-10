// Autore: Timothy Giolito 20054431
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
    // Elenco dei nominativi dei giocatori coinvolti. Per le partite non vincolate a tornei
    // l'identificazione avviene tramite semplici stringhe e non tramite le entità Team del sistema torneistico.
    private List<String> playerNames;
}
