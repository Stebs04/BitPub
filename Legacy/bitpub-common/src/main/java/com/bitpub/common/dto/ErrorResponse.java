package com.bitpub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated(since = "1.0", forRemoval = true)
@Schema(
    name = "ErrorResponse",
    description = "Struttura standardizzata per le risposte di errore dell'API BitPub. (DEPRECATED - Use ApiError invece)"
)
public class ErrorResponse {

    @Schema(
        description = "Codice HTTP dello stato di errore.",
        example = "404",
        allowableValues = {"400", "401", "403", "404", "409", "422", "500"}
    )
    private int status;

    @Schema(
        description = "Tipo di errore HTTP (es. 'Not Found', 'Unauthorized').",
        example = "Not Found"
    )
    private String error;

    @Schema(
        description = "Messaggio di errore leggibile dall'utente con dettagli specifici.",
        example = "Torneo non trovato con id: 550e8400-e29b-41d4-a716-000000000000"
    )
    private String message;

    @Schema(
        description = "Path dell'endpoint che ha generato l'errore.",
        example = "/api/v1/tournaments/550e8400-e29b-41d4-a716-000000000000"
    )
    private String path;

    @Schema(
        description = "Timestamp ISO-8601 del momento in cui si è verificato l'errore.",
        example = "2024-05-24T21:00:00",
        format = "date-time"
    )
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
