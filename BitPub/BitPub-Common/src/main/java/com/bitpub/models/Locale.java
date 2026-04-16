package com.bitpub.models;

// Permette di marcare esplicitamente quali campi includere nel JSON
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;

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
    public Locale(){

    }

    /**
     * Restituisce l'indirizzo IP del nodo Edge.
     * @return String ipAddressEdge
     */
    public String getIpAddressEdge() {
        return ipAddressEdge;
    }

    public void setIpAddressEdge(String ipAddressEdge) {
        this.ipAddressEdge = ipAddressEdge;
    }

    /**
     * Restituisce l'identificativo univoco del locale.
     * @return int id
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce il nome assegnato al locale.
     * @return String name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
