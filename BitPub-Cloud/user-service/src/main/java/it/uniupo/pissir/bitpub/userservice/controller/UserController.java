package it.uniupo.pissir.bitpub.userservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import it.uniupo.pissir.bitpub.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Creazione utente con ruolo assegnato: riservata a PLATFORM_ADMIN.
    // Il ruolo del chiamante arriva dal gateway (JwtAuthenticationFilter) nell'header X-User-Role.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request,
                               @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        if (!"PLATFORM_ADMIN".equals(callerRole)) {
            throw new BitpubException("Only PLATFORM_ADMIN can create users", HttpStatus.FORBIDDEN);
        }
        return userService.createUser(request);
    }

    @PostMapping("/ensure")
    @ResponseStatus(HttpStatus.OK)
    public UserDto ensureUser(@Valid @RequestBody it.uniupo.pissir.bitpub.userservice.dto.EnsureUserRequest request) {
        return userService.ensureUser(request.getUsername());
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable("id") String id) {
        return userService.getUserById(id);
    }
    
    @GetMapping("/by-username/{username}")
    public UserDto getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/count")
    public long countTotalUsers() {
        return userService.countTotalUsers();
    }

    @GetMapping("/by-role/{role}")
    public List<UserDto> getUsersByRole(@PathVariable("role") String role) {
        return userService.getUsersByRole(role);
    }

    @PatchMapping("/{id}/role")
    public UserDto updateUserRole(@PathVariable("id") String id, @RequestParam String role) {
        return userService.updateUserRole(id, role);
    }
}
