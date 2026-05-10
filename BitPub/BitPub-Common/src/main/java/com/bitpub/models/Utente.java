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
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Entità di dominio rappresentante l'utente nel sistema.
 * Implementa UserDetails per l'integrazione nativa con Spring Security,
 * consentendo all'entità di agire direttamente come Principal nel contesto di sicurezza.
 *
 * @author Stefano Bellan 20054330
 */
@Entity
@Table(name = "utenti")
public class Utente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nome;
    private String cognome;
    private int anni;
    private Double credito;
    private Boolean attivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Ruolo role;

    @Transient
    @Expose
    @SerializedName("_links")
    private Map<String, Link> links;

    public Utente() {}

    public Utente(Long id, String username, String email, String password, String nome, String cognome, int anni, Double credito, Boolean attivo, Ruolo role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.anni = anni;
        this.credito = credito;
        this.attivo = attivo;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsernameField() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public int getAnni() { return anni; }
    public void setAnni(int anni) { this.anni = anni; }

    public Double getCredito() { return credito; }
    public void setCredito(Double credito) { this.credito = credito; }

    public Boolean isAttivo() { return attivo; }
    public void setAttivo(Boolean attivo) { this.attivo = attivo; }

    public Ruolo getRuolo() { return role; }
    public void setRuolo(Ruolo role) { this.role = role; }

    public Map<String, Link> getLinks() { return links; }
    public void setLinks(Map<String, Link> links) { this.links = links; }

    // Helper methods for UtenteService
    public String getRole() {
        return role != null ? role.name() : null;
    }

    public void setRole(String roleName) {
        if (roleName == null) {
            this.role = null;
        } else {
            try {
                this.role = Ruolo.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.role = Ruolo.UTENTE_BASE; // Default fallback
            }
        }
    }

    /**
     * Converte il ruolo interno dell'utente in una collezione di autorità per Spring Security.
     * Segue la convenzione del prefisso ROLE_ per garantire la compatibilità con @PreAuthorize.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role.name() : "UTENTE_BASE")));
    }

    /**
     * Utilizza il campo username per il processo di autenticazione.
     */
    @Override
    public String getUsername() {
        return username;
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
        return attivo != null ? attivo : true;
    }

    public enum Ruolo {
        UTENTE_BASE,
        GESTORE,
        ADMIN
    }
}