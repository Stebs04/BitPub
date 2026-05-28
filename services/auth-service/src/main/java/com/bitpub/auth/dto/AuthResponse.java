package com.bitpub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    name = "AuthResponse",
    description = "Risposta di autenticazione contenente il token JWT e i dati dell'utente autenticato."
)
public class AuthResponse {

    @Schema(
        description = "Token JWT firmato con HS256. Valido per 24 ore dalla generazione.",
        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpb19yb3NzaSIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzE2NTc2MDAwLCJleHAiOjE3MTY2NjI0MDB9.abc123xyz"
    )
    private String token;

    @Schema(
        description = "Schema del token. Sempre 'Bearer' per JWT.",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    @Builder.Default
    private String type = "Bearer";

    @Schema(
        description = "Username dell'utente autenticato.",
        example = "mario_rossi"
    )
    private String username;

    @Schema(
        description = "Ruolo assegnato all'utente nel sistema (USER o ADMIN).",
        example = "USER",
        allowableValues = {"USER", "ADMIN"}
    )
    private String role;
}
