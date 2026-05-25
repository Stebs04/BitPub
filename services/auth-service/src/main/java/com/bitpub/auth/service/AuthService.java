package com.bitpub.auth.service;

import com.bitpub.auth.dto.AuthRequest;
import com.bitpub.auth.dto.AuthResponse;
import com.bitpub.auth.dto.RegisterRequest;

import com.bitpub.auth.model.User;
import com.bitpub.auth.repository.UserRepository;
import com.bitpub.auth.security.CustomUserDetailsService;
import com.bitpub.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final com.bitpub.auth.repository.RoleRepository roleRepository;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new com.bitpub.common.exception.ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.bitpub.common.exception.ConflictException("Email is already taken");
        }

        com.bitpub.auth.model.Role playerRole = roleRepository.findByName(com.bitpub.common.security.enums.Role.PLAYER)
                .orElseThrow(() -> new com.bitpub.common.exception.ResourceNotFoundException("Error: Role PLAYER is not found."));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .roles(java.util.Collections.singleton(playerRole)) // Default role
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String roleName = user.getRoles().iterator().next().getName().name();
        String jwtToken = jwtUtil.generateToken(userDetails, roleName);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .role(roleName)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new com.bitpub.common.exception.ResourceNotFoundException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String roleName = user.getRoles().iterator().next().getName().name();
        String jwtToken = jwtUtil.generateToken(userDetails, roleName);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .role(roleName)
                .build();
    }
}
