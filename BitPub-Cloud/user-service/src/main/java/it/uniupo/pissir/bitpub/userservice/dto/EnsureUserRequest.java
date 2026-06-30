package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnsureUserRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;
}
