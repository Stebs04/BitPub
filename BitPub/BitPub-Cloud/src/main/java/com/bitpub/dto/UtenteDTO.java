package com.bitpub.dto;

public class UtenteDTO {
    private Long id;
    private String username;
    private String role;
    private String nome;
    private String cognome;
    private int anni;
    private String email;
    private Double credito;
    private Boolean attivo;

    // Password is usually omitted from DTOs or handled differently depending on the request type (create vs read)
    private String password;

    public UtenteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    
    public int getAnni() { return anni; }
    public void setAnni(int anni) { this.anni = anni; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Double getCredito() { return credito; }
    public void setCredito(Double credito) { this.credito = credito; }
    
    public Boolean getAttivo() { return attivo; }
    public void setAttivo(Boolean attivo) { this.attivo = attivo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
