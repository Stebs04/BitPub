package com.bitpub.dto;

public class LocaleDTO {
    private Long id;
    private String nome;
    private String ipAddressEdge;
    private String indirizzo;
    private String citta;
    private Integer capienza;
    private Long gestoreId;

    public LocaleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getIpAddressEdge() { return ipAddressEdge; }
    public void setIpAddressEdge(String ipAddressEdge) { this.ipAddressEdge = ipAddressEdge; }

    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    public String getCitta() { return citta; }
    public void setCitta(String citta) { this.citta = citta; }

    public Integer getCapienza() { return capienza; }
    public void setCapienza(Integer capienza) { this.capienza = capienza; }

    public Long getGestoreId() { return gestoreId; }
    public void setGestoreId(Long gestoreId) { this.gestoreId = gestoreId; }
}
