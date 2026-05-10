package com.bitpub.services;

import com.bitpub.cloud.security.JwtService;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UtenteRepository utenteRepository;
    
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(authenticationManager, jwtService, utenteRepository, passwordEncoder);
    }

    @Test
    void authenticate_Successful() {
        // Arrange
        String username = "testuser";
        String password = "password";
        AuthRequest request = new AuthRequest(username, password);
        
        UserDetails userDetails = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .authorities("ROLE_UTENTE_BASE")
                .build();
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-token");

        // Act
        AuthResponse response = authService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals("mock-token", response.getToken());
        verify(authenticationManager).                     authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_BadCredentials() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        AuthRequest request = new AuthRequest(username, password);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(request));
    }
}
