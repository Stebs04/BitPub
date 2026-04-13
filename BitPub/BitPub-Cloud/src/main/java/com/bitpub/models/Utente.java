package com.bitpub.models;

//Importazione di bcrypt per hashing della password
import org.mindrot.jbcrypt.BCrypt;

/**
 * Rappresenta un Utente all'interno del dominio applicativo.
 * Questa classe modello contiene le informazioni di base per l'autenticazione
 * e la gestione delle autorizzazioni.
 * @author: Stefano Bellan 20054330
 */
public class Utente {

    /** L'identificativo univoco dell'utente, tipicamente generato dal database. */
    private Long id;

    /** Il nome utente scelto per la visualizzazione e/o l'accesso. */
    private String nickname;

    /** Il livello di privilegio assegnato all'utente (es. ADMIN, USER). */
    private String ruolo;

    /** Il nome anagrafico dell'utente. */
    private String nome;

    /** Il cognome anagrafico dell'utente. */
    private String cognome;

    /** L'età dell'utente, espressa in anni. */
    private int anni;

    /** L'indirizzo e-mail dell'utente, utilizzato per comunicazioni e recupero account. */
    private String email;

    /** La credenziale di accesso. */
    private String password;

    /**
     * Costruisce un nuovo oggetto Utente.
     * Effettua una validazione di base per assicurarsi che nickname e ruolo non siano nulli o vuoti.
     *
     * @param nickname il nome utente da assegnare
     * @param ruolo il ruolo associato all'utente
     * @param nome il nome dell'utente
     * @param cognome il cognome dell'utente
     * @param email l'indirizzo email dell'utente
     * @param password la password dell'utente
     */
    public Utente(String nickname, String ruolo, String nome, String cognome, String email, String password) {
        // Verifica che i campi obbligatori (nickname e ruolo) non siano nulli o composti solo da spazi
        if (nickname == null || nickname.isBlank() || ruolo == null || ruolo.isBlank()) {
            // Lancia un'eccezione se i requisiti minimi di validazione non sono rispettati
            throw new IllegalArgumentException("Campi nickname e ruolo mancanti!!!");
        }

        // Assegna i valori validati alle variabili di istanza della classe
        this.nickname = nickname;
        this.ruolo = ruolo;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;

        // Genera un "salt" casuale e cifra la password in chiaro usando l'algoritmo BCrypt
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Restituisce il nickname dell'utente.
     * @return il nickname attuale
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Imposta un nuovo nickname per l'utente.
     * @param nickname il nuovo nickname da impostare
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Restituisce il ruolo dell'utente.
     * @return il ruolo attuale
     */
    public String getRuolo() {
        return ruolo;
    }

    /**
     * Imposta un nuovo ruolo per l'utente.
     * @param ruolo il nuovo ruolo da assegnare
     */
    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    /**
     * Restituisce l'identificativo univoco dell'utente.
     * @return l'id attuale, oppure null se l'entità non è ancora stata persistita
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'identificativo univoco dell'utente.
     * @param id l'identificativo da assegnare
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce l'indirizzo e-mail associato all'utente.
     * @return l'email attuale
     */
    public String getEmail() {
        return email;
    }

    /**
     * Imposta un nuovo indirizzo e-mail per l'utente.
     * @param email il nuovo indirizzo e-mail da salvare
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Restituisce la password associata all'utente.
     * @return la password attuale (generalmente memorizzata come hash)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Aggiorna la password dell'utente applicando l'hashing di sicurezza.
     * Prende la stringa in chiaro, la cifra e salva il risultato nel campo di istanza.
     *
     * @param password la nuova password in chiaro da sottoporre a hashing
     */
    public void setPassword(String password) {
        // Genera un nuovo salt casuale e trasforma la password in chiaro in un hash BCrypt
        // Il risultato salvato in this.password sarà una stringa sicura non reversibile
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verifica se una password fornita in chiaro corrisponde all'hash salvato.
     * Include un controllo preliminare per evitare errori con valori nulli o vuoti.
     *
     * @param passwordInChiaro la password da verificare (es. quella digitata nel login)
     * @return true se la password è corretta, false se è errata, nulla o vuota
     */
    public boolean checkPassword(String passwordInChiaro) {
        // Controllo preventivo: se la password passata è nulla o composta solo da spazi, non è valida
        if (passwordInChiaro == null || passwordInChiaro.isBlank()) {
            // Ritorna false immediatamente senza tentare il confronto con l'hash
            return false;
        }

        // Usa BCrypt per confrontare la password in chiaro con l'hash memorizzato in this.password
        // Il metodo checkpw estrae il salt dall'hash esistente per ricalcolare il confronto
        return BCrypt.checkpw(passwordInChiaro, this.password);
    }

    /**
     * Restituisce il cognome anagrafico dell'utente.
     * @return il cognome
     */
    public String getCognome() {
        return cognome;
    }

    /**
     * Restituisce il nome anagrafico dell'utente.
     * @return il nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * Restituisce l'età dell'utente.
     * @return gli anni
     */
    public int getAnni() {
        return anni;
    }

    /**
     * Restituisce una rappresentazione testuale dell'oggetto.
     * @return una stringa contenente lo stato corrente dei campi dell'utente
     */
    @Override
    public String toString() {
        return "Utente{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", ruolo='" + ruolo + '\'' +
                '}';
    }
}