package com.bitpub.controllers;

import com.bitpub.dto.UtenteDTO;
import com.bitpub.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    @Autowired
    private UtenteService utenteService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UtenteDTO>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {

        List<UtenteDTO> utenti = utenteService.cercaUtenti(role, search);
        return ResponseEntity.ok(utenti);
    }

    @PutMapping("/{username}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable String username) {
        if (utenteService.toggleUserStatus(username)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{username}/toggle-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserRole(@PathVariable String username) {
        return utenteService.toggleUserRole(username)
                .map(result -> {
                    if ("ADMIN_ROLE_PROTECTED".equals(result)) {
                        return ResponseEntity.badRequest().body("Impossibile modificare il ruolo di un ADMIN.");
                    }
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
