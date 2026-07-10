package it.uniupo.pissir.bitpub.userservice.controller;

import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import it.uniupo.pissir.bitpub.userservice.dto.VerifyCredentialsRequest;
import it.uniupo.pissir.bitpub.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Autore: Luca Franzon 20054744
 * 
 * Controller REST che espone gli endpoint per la gestione del ciclo di vita degli utenti.
 * Fornisce operazioni CRUD riservate agli amministratori di piattaforma, oltre a 
 * servizi essenziali per l'autenticazione interna tra microservizi.
 */

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Gestione utenti (CRUD completo): riservata a PLATFORM_ADMIN.
    // Il ruolo arriva dal JWT inoltrato dal gateway ed e' valorizzato nel SecurityContext
    // dal JwtAuthenticationFilter condiviso (bitpub-common).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    // Verifica username/password contro l'hash memorizzato: usato dall'auth-service per il login.
    @PostMapping("/verify")
    public UserDto verifyCredentials(@Valid @RequestBody VerifyCredentialsRequest request) {
        return userService.verifyCredentials(request);
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
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/count")
    public long countTotalUsers() {
        return userService.countTotalUsers();
    }

    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<UserDto> getUsersByRole(@PathVariable("role") String role) {
        return userService.getUsersByRole(role);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserDto updateUser(@PathVariable("id") String id,
                              @Valid @RequestBody it.uniupo.pissir.bitpub.userservice.dto.UpdateUserRequest request) {
        return userService.updateUserDetails(id, request);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserDto updateUserPassword(@PathVariable("id") String id,
                                      @Valid @RequestBody it.uniupo.pissir.bitpub.userservice.dto.UpdatePasswordRequest request) {
        return userService.updateUserPassword(id, request);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public UserDto updateUserRole(@PathVariable("id") String id, @RequestParam String role) {
        return userService.updateUserRole(id, role);
    }

    // Chiamata interna da locale-service (service-to-service, come /ensure) quando un
    // LOCALE_ADMIN viene assegnato a un locale, per sincronizzare il localeId sull'utente.
    @PatchMapping("/{id}/locale")
    public UserDto updateUserLocale(@PathVariable("id") String id, @RequestParam String localeId) {
        return userService.updateUserLocale(id, localeId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public void deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
    }
}
