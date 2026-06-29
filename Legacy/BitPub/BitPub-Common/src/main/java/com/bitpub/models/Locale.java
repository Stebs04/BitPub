package com.bitpub.models;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;

/**
 * Rappresenta un'entità Locale all'interno del sistema.
 * Gestisce le informazioni anagrafiche e di rete associate a un nodo periferico (Edge).
 * @author Stefano Bellan 20054330
 */
@Entity
@Table(name = "locali")
public class Locale extends ResourceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private Long id;

    @Column(nullable = false)
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

    public Locale() {
    }

    public Locale(String name, String ipAddressEdge) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campo nome mancante!!!");
        }
        if (ipAddressEdge == null || ipAddressEdge.isBlank()) {
            throw new IllegalArgumentException("Campo ip mancante!!!");
        }
        this.name = name;
        this.ipAddressEdge = ipAddressEdge;
    }

    public String getIpAddressEdge() { return ipAddressEdge; }
    public void setIpAddressEdge(String ipAddressEdge) { this.ipAddressEdge = ipAddressEdge; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    public String getCitta() { return citta; }
    public void setCitta(String citta) { this.citta = citta; }

    public Integer getCapienza() { return capienza; }
    public void setCapienza(Integer capienza) { this.capienza = capienza; }

    public Long getGestoreId() { return gestoreId; }
    public void setGestoreId(Long gestoreId) { this.gestoreId = gestoreId; }
}
