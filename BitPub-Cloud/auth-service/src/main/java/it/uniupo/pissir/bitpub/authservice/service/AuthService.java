/**
 * Autore: Luca Franzon 20054744
 * Servizio principale per l'autenticazione. Gestisce la comunicazione con
 * il microservizio utente per la validazione delle credenziali e si occupa
 * della creazione e firma dei token JWT.
 */
package it.uniupo.pissir.bitpub.authservice.service;

import it.uniupo.pissir.bitpub.authservice.dto.JwtResponse;
import it.uniupo.pissir.bitpub.authservice.dto.LoginRequest;
import it.uniupo.pissir.bitpub.authservice.dto.UserDto;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtils jwtUtils;
    private final RestClient restClient;

    @Value("${bitpub.services.user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    /**
     * Esegue il processo di autenticazione verificando le credenziali fornite.
     * In caso di successo, procede alla generazione di un token JWT valido per le 
     * successive richieste autenticate.
     *
     * @param request i dati di login inviati dal client
     * @return un oggetto JwtResponse contenente il token e i dettagli base dell'utente
     * @throws BitpubException se le credenziali risultano non valide o la comunicazione remota fallisce
     */
    public JwtResponse login(LoginRequest request) {
        /*
         * Viene effettuata una chiamata HTTP verso il servizio utente per convalidare 
         * la combinazione di username e password. La risposta conterrà i dati aggiornati dell'utente.
         */
        UserDto user;
        try {
            user = restClient.post()
                    .uri(userServiceUrl + "/api/v1/users/verify")
                    .body(request)
                    .retrieve()
                    .body(UserDto.class);
        } catch (Exception e) {
            log.error("Login failed for user-service call", e);
            throw new BitpubException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getId(), user.getLocaleId());

        return JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .localeId(user.getLocaleId())
                .build();
    }
}
