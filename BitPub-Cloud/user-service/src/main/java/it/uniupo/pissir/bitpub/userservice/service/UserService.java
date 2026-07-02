package it.uniupo.pissir.bitpub.userservice.service;

import it.uniupo.pissir.bitpub.common.dto.Role;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.userservice.domain.User;
import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import it.uniupo.pissir.bitpub.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BitpubException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BitpubException("Email already exists", HttpStatus.CONFLICT);
        }
        try {
            Role.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new BitpubException("Invalid role: " + request.getRole(), HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(request.getPassword()) // Assumendo che password sia l'hash, oppure fare hashing qui
                .email(request.getEmail())
                .role(request.getRole())
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    public UserDto ensureUser(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToDto)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .passwordHash("arcade_password_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                            .email(username.toLowerCase().replaceAll("\\s+", "") + "@bitpub.local")
                            .role("PLAYER")
                            .createdAt(Instant.now())
                            .build();
                    return mapToDto(userRepository.save(newUser));
                });
    }

    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDto(user);
    }
    
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public long countTotalUsers() {
        return userRepository.count();
    }

    public List<UserDto> getUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole().equalsIgnoreCase(role))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public UserDto updateUserRole(String id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRole(role);
        return mapToDto(userRepository.save(user));
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
