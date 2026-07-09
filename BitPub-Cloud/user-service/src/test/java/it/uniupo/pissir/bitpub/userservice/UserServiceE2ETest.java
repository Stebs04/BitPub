package it.uniupo.pissir.bitpub.userservice;

import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class UserServiceE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // Opzionalmente pulire il DB o settare stato iniziale prima di ogni test se non rollbackato
    }

    // POST /users e' protetto da @PreAuthorize('PLATFORM_ADMIN'): serve un JWT admin
    // firmato con la stessa chiave del JwtAuthenticationFilter condiviso (bitpub-common).
    private HttpHeaders adminAuth() {
        String token = jwtUtils.generateToken("admin", "PLATFORM_ADMIN", "admin-id", null);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void createUserAndFetch_E2E_Success() {
        // 1. Create User
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("e2e_user");
        request.setEmail("e2e@test.com");
        request.setPassword("securepass");
        request.setRole("PLAYER");

        ResponseEntity<UserDto> createResponse = restTemplate.exchange(
                "/api/v1/users", HttpMethod.POST, new HttpEntity<>(request, adminAuth()), UserDto.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        UserDto createdUser = createResponse.getBody();
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("e2e_user", createdUser.getUsername());
        
        // 2. Fetch User by ID
        ResponseEntity<UserDto> fetchResponse = restTemplate.getForEntity("/api/v1/users/" + createdUser.getId(), UserDto.class);
        
        assertEquals(HttpStatus.OK, fetchResponse.getStatusCode());
        UserDto fetchedUser = fetchResponse.getBody();
        assertNotNull(fetchedUser);
        assertEquals("e2e_user", fetchedUser.getUsername());
    }
}
