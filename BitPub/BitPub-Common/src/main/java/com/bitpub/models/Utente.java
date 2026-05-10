package com.bitpub.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entità di dominio rappresentante l'utente nel sistema.
 * Implementa UserDetails per l'integrazione nativa con Spring Security,
 * consentendo all'entità di agire direttamente come Principal nel contesto di sicurezza.
 *
 * @author Senior Software Engineer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "utenti")
public class Utente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nome;
    private String cognome;

    @Enumerated(EnumType.STRING)
    private Ruolo ruolo;

    /**
     * Converte il ruolo interno dell'utente in una collezione di autorità per Spring Security.
     * Segue la convenzione del prefisso ROLE_ per garantire la compatibilità con @PreAuthorize.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + ruolo.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Utilizza l'email come username univoco per il processo di autenticazione.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public enum Ruolo {
        USER,
        GESTORE,
        ADMIN
    }
}