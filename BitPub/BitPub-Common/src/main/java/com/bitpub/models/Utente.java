package com.bitpub.models;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import com.google.gson.annotations.Expose;

/**
 * Entità di dominio rappresentante l'utente nel sistema.
 * Implementa UserDetails per l'integrazione nativa con Spring Security.
 */
@Entity
@Table(name = "utenti")
public class Utente extends ResourceModel implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private Long id;

    @Column(nullable = false, unique = true)
    @Expose
    private String username;

    @Column(nullable = false, unique = true)
    @Expose
    private String email;

    @Column(nullable = false)
    private String password;

    @Expose
    private String nome;
    
    @Expose
    private String cognome;
    
    @Expose
    private int anni;
    
    @Expose
    private Double credito;
    
    @Expose
    private Boolean attivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Expose
    private Ruolo role;

    public Utente() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
                this.role = Ruolo.UTENTE_BASE;
            }
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role.name() : "UTENTE_BASE")));
    }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return attivo != null ? attivo : true; }

    public enum Ruolo {
        UTENTE_BASE,
        GESTORE,
        ADMIN
    }
}
