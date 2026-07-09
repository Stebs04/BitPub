package it.uniupo.pissir.bitpub.userservice.repository;

import it.uniupo.pissir.bitpub.userservice.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// H2-backed slice: portable, no Docker. Testcontainers/Postgres lives in the E2E suite.
@DataJpaTest
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .username("dbtestuser")
                .email("dbtest@test.com")
                .passwordHash("hash")
                .role("PLAYER")
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
    }

    @Test
    void testFindByUsername_Success() {
        Optional<User> user = userRepository.findByUsername("dbtestuser");
        assertTrue(user.isPresent());
        assertEquals("dbtest@test.com", user.get().getEmail());
    }

    @Test
    void testFindByUsername_NotFound() {
        assertTrue(userRepository.findByUsername("ghost").isEmpty());
    }

    @Test
    void testExistsByEmail_Success() {
        assertTrue(userRepository.existsByEmail("dbtest@test.com"));
    }

    @Test
    void testExistsByEmail_Failure() {
        assertFalse(userRepository.existsByEmail("notfound@test.com"));
    }

    @Test
    void testExistsByUsername() {
        assertTrue(userRepository.existsByUsername("dbtestuser"));
        assertFalse(userRepository.existsByUsername("ghost"));
    }
}
