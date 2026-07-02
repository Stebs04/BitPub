package it.uniupo.pissir.bitpub.userservice.config;

import it.uniupo.pissir.bitpub.common.dto.Role;
import it.uniupo.pissir.bitpub.common.security.PasswordUtils;
import it.uniupo.pissir.bitpub.userservice.domain.User;
import it.uniupo.pissir.bitpub.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;

    private static final String DEFAULT_PASSWORD = "password123";

    @Override
    public void run(String... args) {
        for (Role role : Role.values()) {
            String email = role.name().toLowerCase() + "@test.com";
            // Idempotente: se il seed esiste gia' non va ricreato, altrimenti perderebbe l'id
            // ad ogni riavvio, orfanizzando i locali gia' assegnati a un LOCALE_ADMIN esistente.
            if (userRepository.findByEmail(email).isPresent()) {
                continue;
            }

            User user = User.builder()
                    .username(role.name().toLowerCase())
                    .email(email)
                    .passwordHash(passwordUtils.hash(DEFAULT_PASSWORD))
                    .role(role.name())
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(user);
        }
    }
}
