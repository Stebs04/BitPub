package com.bitpub.models;

// Permette di marcare esplicitamente quali campi includere nel JSON
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import java.util.Map;

/**
 * Rappresenta un'entità Locale all'interno del sistema.
 * Gestisce le informazioni anagrafiche e di rete associate a un nodo periferico (Edge).
 * @author Stefano Bellan 20054330
 */
@Entity // Indica a JPA che questa classe è una tabella del DB
@Table(name = "locali") // Specifica il nome della tabella
public class Locale {

    @Id // Chiave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private Long id;

    @Column(nullable = false) // Il nome non può essere nullo nel DB
    @Expose
    private String name;

    @Column(name = "ip_address_edge", nullable = false)
    @Expose
    private String ipAddressEdge;

    @Column
    @Expose
    private String indirizzo;

    @Column
    @Expose
    private String citta;

    @Column
    @Expose
    private Integer capienza;

    @Column(name = "gestore_id")
    @Expose
    private Long gestoreId;

    // L'annotazione @Transient dice a JPA di ignorare questo campo nel database.
    // L'annotazione @Expose permette a GSON di leggerlo dalla risposta di rete.
    @Transient
    @Expose
    private Map<String, Link> _links; // <-- Modifica stilistica qui

    /**
     * Costruttore completo per la classe Locale.
     *
     * @param name          Il nome descrittivo del locale.
     * @param ipAddressEdge  L'indirizzo IP del server edge associato.
     * @throws IllegalArgumentException se il nome o l'IP sono nulli o stringhe vuote.
     */
    public Locale(String name, String ipAddressEdge) {
        // Validazione input: garantisce l'integrità dei dati obbligatori
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campo nome mancante!!!");
        }
        if (ipAddressEdge == null || ipAddressEdge.isBlank()) {
            throw new IllegalArgumentException("Campo ip mancante!!!");
        }

        this.name = name;
        this.ipAddressEdge = ipAddressEdge;
    }

    /**
     * Costruttore senza argomenti (No-Args).
     * Necessario per le librerie di framework (come GSON e JPA) che creano
     * l'istanza tramite riflessione prima di popolarne i campi.
     */
    public Locale() {
    }

    public String getIpAddressEdge() {
        return ipAddressEdge;
    }

    public void setIpAddressEdge(String ipAddressEdge) {
        this.ipAddressEdge = ipAddressEdge;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public Integer getCapienza() {
        return capienza;
    }

    public void setCapienza(Integer capienza) {
        this.capienza = capienza;
    }

    public Long getGestoreId() {
        return gestoreId;
    }

    public void setGestoreId(Long gestoreId) {
        this.gestoreId = gestoreId;
    }

    /**
     * Metodi richiesti per la navigazione HATEOAS.
     * Restituiscono o impostano la mappa dei link inviata dal server.
     */
    public Map<String, Link> get_links() { // <-- Modifica stilistica qui
        return _links;
    }

    public void set_links(Map<String, Link> _links) { // <-- Modifica stilistica qui
        this._links = _links;
    }
}