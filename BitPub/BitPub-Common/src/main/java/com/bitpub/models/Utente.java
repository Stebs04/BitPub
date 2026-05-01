package com.bitpub.models;

import org.springframework.security.crypto.bcrypt.BCrypt;
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;

/**
 * Rappresenta un Utente all'interno del dominio applicativo BitPub.
 * Questa entità gestisce le informazioni anagrafiche, le credenziali di accesso
 * cifrate e il bilancio economico del profilo.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Entity
@Table(name = "utenti")
public class Utente extends ResourceModel {

    /** Identificativo univoco generato automaticamente dal sistema di persistenza. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private Long id;

    /** Nome utente univoco utilizzato per l'identificazione nel sistema. */
    @Column(nullable = false, unique = true)
    @Expose
    private String username;

    /** Ruolo associato all'utente per la gestione dei permessi (es. ADMIN, GESTORE, UTENTE). */
    @Column(nullable = false, columnDefinition = "varchar(255) default 'USER'")
    @Expose
    private String role = "USER";

    /** Nome anagrafico del titolare del profilo. */
    @Expose
    private String nome;

    /** Cognome anagrafico del titolare del profilo. */
    @Expose
    private String cognome;

    /** Età dell'utente registrata per fini statistici o legali. */
    private int anni;

    /** Indirizzo e-mail univoco associato all'account. */
    @Column(unique = true)
    @Expose
    private String email;

    /** Hash della password generato tramite algoritmo BCrypt. Non esposto nelle API JSON. */
    @Column(nullable = false)
    private String password;

    /** Credito residuo disponibile per l'utilizzo dei servizi. */
    @Column(nullable = false, columnDefinition = "float8 default 0.0")
    @Expose
    private Double credito = 0.0;

    /** Flag di stato dell'account per inibire l'accesso (Attivo/Sospeso). */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Expose
    private Boolean attivo = true;

    /**
     * Costruttore completo per la creazione di un nuovo utente.
     * Implementa la cifratura immediata della password e la validazione dei campi obbligatori.
     *
     * @param username Identificativo scelto dall'utente.
     * @param role     Livello di accesso assegnato.
     * @param nome     Nome anagrafico.
     * @param cognome  Cognome anagrafico.
     * @param email    Indirizzo e-mail di contatto.
     * @param password Password in chiaro da sottoporre a hashing.
     */
    public Utente(String username, String role, String nome, String cognome, String email, String password) {
        if (username == null || username.isBlank() || role == null || role.isBlank()) {
            throw new IllegalArgumentException("Campi username e role sono obbligatori.");
        }

        this.username = username;
        this.role = role;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.credito = 0.0;
        this.attivo = true;

        // Cifratura della password tramite salt dinamico
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Costruttore predefinito richiesto dai framework di persistenza (JPA) e serializzazione (GSON).
     */
    public Utente() {}

    /** @return Il nome utente. */
    public String getUsername() { return username; }

    /** @param username Il nuovo nome utente da impostare. */
    public void setUsername(String username) { this.username = username; }

    /** @return Il ruolo dell'utente. */
    public String getRole() { return role; }

    /** @param role Il nuovo ruolo da assegnare. */
    public void setRole(String role) { this.role = role; }

    /** @return L'ID univoco dell'entità. */
    public Long getId() { return id; }

    /** @param id L'identificativo da assegnare. */
    public void setId(Long id) { this.id = id; }

    /** @return L'indirizzo e-mail dell'utente. */
    public String getEmail() { return email; }

    /** @param email La nuova e-mail da associare. */
    public void setEmail(String email) { this.email = email; }

    /** @return L'hash della password memorizzato. */
    public String getPassword() { return password; }

    /**
     * Aggiorna la password applicando l'hashing BCrypt.
     * @param password La nuova password in chiaro.
     */
    public void setPassword(String password) {
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Confronta una password in chiaro con l'hash presente nel database.
     *
     * @param passwordInChiaro La password fornita in fase di login.
     * @return true se la password coincide, false altrimenti.
     */
    public boolean checkPassword(String passwordInChiaro) {
        if (passwordInChiaro == null || passwordInChiaro.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(passwordInChiaro, this.password);
    }

    /** @return Il cognome dell'utente. */
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    /** @return Il nome dell'utente. */
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    /** @return L'età registrata. */
    public int getAnni() { return anni; }
    public void setAnni(int anni) { this.anni = anni; }

    /** @return Il credito disponibile. */
    public Double getCredito() { return credito; }
    public void setCredito(Double credito) { this.credito = credito; }

    /** @return Lo stato di attività dell'account. */
    public Boolean isAttivo() { return attivo; }
    public void setAttivo(Boolean attivo) { this.attivo = attivo; }

    /**
     * Restituisce una stringa descrittiva dello stato attuale (es. "ATTIVO" o "SOSPESO").
     * Utilizzato primariamente per il binding nelle colonne delle TableView JavaFX.
     *
     * @return Lo stato testuale dell'account.
     */
    public String getStato() {
        return (attivo != null && attivo) ? "ATTIVO" : "SOSPESO";
    }

    @Override
    public String toString() {
        return "Utente{" + "id=" + id + ", username='" + username + '\'' + ", role='" + role + '\'' + '}';
    }
}
