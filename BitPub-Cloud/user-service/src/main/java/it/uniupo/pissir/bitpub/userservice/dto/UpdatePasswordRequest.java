package it.uniupo.pissir.bitpub.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "Password is required")
    private String newPassword;
}
