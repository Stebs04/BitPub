package com.bitpub.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "ApiError",
    description = "Struttura standard per gli errori dell'API basata su Problem Details (RFC 7807)"
)
public class ApiError {
    @Schema(description = "Codice HTTP dello stato", example = "404")
    private int status;

    @Schema(description = "URI del tipo di errore", example = "about:blank")
    private String type;

    @Schema(description = "Titolo breve dell'errore", example = "Not Found")
    private String title;

    @Schema(description = "Codice di errore interno", example = "ERR-404")
    private String code;

    @Schema(description = "Messaggio dettagliato dell'errore", example = "Risorsa non trovata")
    private String message;

    @Schema(description = "Dettagli aggiuntivi, es. errori di validazione", example = "{\"field\":\"must not be null\"}")
    private Map<String, Object> details;

    @Schema(description = "Trace ID per correlazione dei log", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    @Schema(description = "Timestamp dell'errore", example = "2026-05-25T15:00:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "URI della richiesta che ha generato l'errore", example = "/api/v1/resource")
    private String path;

    public ProblemDetail toProblemDetail() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), message);
        pd.setType(type != null ? URI.create(type) : URI.create("about:blank"));
        pd.setTitle(title);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", timestamp);
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }
        if (details != null && !details.isEmpty()) {
            pd.setProperty("details", details);
        }
        if (path != null) {
            pd.setProperty("path", path);
        }
        return pd;
    }
}
