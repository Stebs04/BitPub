package it.uniupo.pissir.bitpub.userservice.repository;

import it.uniupo.pissir.bitpub.userservice.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

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

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testConnection() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @Test
    void testFindByUsername_Success() {
        Optional<User> user = userRepository.findByUsername("dbtestuser");
        assertTrue(user.isPresent());
        assertEquals("dbtest@test.com", user.get().getEmail());
    }

    @Test
    void testExistsByEmail_Success() {
        boolean exists = userRepository.existsByEmail("dbtest@test.com");
        assertTrue(exists);
    }
    
    @Test
    void testExistsByEmail_Failure() {
        boolean exists = userRepository.existsByEmail("notfound@test.com");
        assertFalse(exists);
    }
}
