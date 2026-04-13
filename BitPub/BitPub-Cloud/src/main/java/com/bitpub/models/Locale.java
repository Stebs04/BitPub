package com.bitpub.models;

/**
 * Rappresenta un'entità Locale all'interno del sistema.
 * Gestisce le informazioni anagrafiche e di rete associate a un nodo periferico (Edge).
 * @author Stefano Bellan 20054330
 */
public class Locale {

    private Long id;
    private String name;
    private String ipAddressEdge;

    /**
     * Costruttore completo per la classe Locale.
     *
     * @param nome           Il nome descrittivo del locale.
     * @param ipAddressEdge  L'indirizzo IP del server edge associato.
     * @throws IllegalArgumentException se il nome o l'IP sono nulli o stringhe vuote.
     */
    public Locale(String nome, String ipAddressEdge) {
        // Validazione input: garantisce l'integrità dei dati obbligatori
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Campo nome mancante!!!");
        }
        if (ipAddressEdge == null || ipAddressEdge.isBlank()) {
            throw new IllegalArgumentException("Campo ip mancante!!!");
        }

        this.name = nome;
        this.ipAddressEdge = ipAddressEdge;
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
