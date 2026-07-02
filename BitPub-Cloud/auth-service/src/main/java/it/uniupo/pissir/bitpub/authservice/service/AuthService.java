package it.uniupo.pissir.bitpub.authservice.service;

import it.uniupo.pissir.bitpub.authservice.dto.JwtResponse;
import it.uniupo.pissir.bitpub.authservice.dto.LoginRequest;
import it.uniupo.pissir.bitpub.authservice.dto.UserDto;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtils jwtUtils;
    private final RestClient restClient;

    @Value("${bitpub.services.user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    public JwtResponse login(LoginRequest request) {
        // Chiama user-service per verificare username/password contro l'hash memorizzato
        UserDto user;
        try {
            user = restClient.post()
                    .uri(userServiceUrl + "/api/v1/users/verify")
                    .body(request)
                    .retrieve()
                    .body(UserDto.class);
        } catch (Exception e) {
            throw new BitpubException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getId());

        return JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
