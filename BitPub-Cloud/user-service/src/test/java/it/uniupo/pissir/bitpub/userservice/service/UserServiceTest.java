package it.uniupo.pissir.bitpub.userservice.service;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.common.security.PasswordUtils;
import it.uniupo.pissir.bitpub.userservice.domain.User;
import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import it.uniupo.pissir.bitpub.userservice.dto.VerifyCredentialsRequest;
import it.uniupo.pissir.bitpub.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordUtils passwordUtils;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createUserRequest;
    private User user;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@test.com");
        createUserRequest.setPassword("password");
        createUserRequest.setRole("PLAYER");

        user = User.builder()
                .id("1")
                .username("testuser")
                .email("test@test.com")
                .role("PLAYER")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordUtils.hash("password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto response = userService.createUser(createUserRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(userRepository, times(1)).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
    }

    @Test
    void createUser_InvalidRole_ThrowsBadRequest() {
        createUserRequest.setRole("WIZARD");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        BitpubException ex = assertThrows(BitpubException.class, () -> userService.createUser(createUserRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyCredentials_ValidPassword_ReturnsUser() {
        user.setPasswordHash("hash");
        VerifyCredentialsRequest req = new VerifyCredentialsRequest();
        req.setUsername("testuser");
        req.setPassword("password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordUtils.matches("password", "hash")).thenReturn(true);

        UserDto response = userService.verifyCredentials(req);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void verifyCredentials_WrongPassword_ThrowsUnauthorized() {
        user.setPasswordHash("hash");
        VerifyCredentialsRequest req = new VerifyCredentialsRequest();
        req.setUsername("testuser");
        req.setPassword("wrong");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordUtils.matches("wrong", "hash")).thenReturn(false);

        BitpubException ex = assertThrows(BitpubException.class, () -> userService.verifyCredentials(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void verifyCredentials_UnknownUser_ThrowsUnauthorized() {
        VerifyCredentialsRequest req = new VerifyCredentialsRequest();
        req.setUsername("ghost");
        req.setPassword("x");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BitpubException ex = assertThrows(BitpubException.class, () -> userService.verifyCredentials(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void deleteUser_Missing_ThrowsNotFound() {
        when(userRepository.existsById("nope")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser("nope"));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BitpubException exception = assertThrows(BitpubException.class, () -> userService.createUser(createUserRequest));
        assertEquals("Username already exists", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        UserDto response = userService.getUserById("1");

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("1"));
    }
}
