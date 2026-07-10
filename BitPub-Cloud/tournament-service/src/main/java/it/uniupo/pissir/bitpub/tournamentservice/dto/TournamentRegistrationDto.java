/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object per l'iscrizione a un torneo.
 * Espone all'esterno i dettagli della registrazione, includendo statistiche in tempo reale sul partecipante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistrationDto {
    private String id;
    private String tournamentId;
    private String participantId;
    private String participantName;
    private boolean team;          // Specifica se si tratta di un'iscrizione a squadre
    private List<String> members;  // Username o ID dei membri associati (o del singolo partecipante)
    private String teamId;         // Identificativo dell'entità Team, popolato unicamente per tornei a squadre
    private String localeId;
    private Instant registeredAt;

    // Statistiche specifiche per il partecipante limitatamente a questo torneo, calcolate dinamicamente dal tabellone.
    private int matchesPlayed;    // Numero di incontri portati a termine in questa competizione
    private int goalsScored;      // Somma dei gol realizzati durante gli scontri del torneo
    private String currentStage;  // Livello più alto raggiunto nel tabellone (es. Semifinale, Finale)
    private String tournamentStatus; // Stato attuale del torneo, utile al client per filtrare le competizioni già concluse
}
