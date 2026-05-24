package com.bitpub.services;

import com.bitpub.dto.UtenteDTO;
import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UtenteService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<UtenteDTO> getUtenteById(Long id) {
        return utenteRepository.findById(id).map(this::convertToDTO);
    }

    public List<UtenteDTO> getAllUtenti(String ruolo) {
        List<Utente> utenti;
        if (ruolo != null && !ruolo.trim().isEmpty()) {
            try {
                utenti = utenteRepository.findByRole(Utente.Ruolo.valueOf(ruolo.toUpperCase()));
            } catch (IllegalArgumentException e) {
                utenti = List.of();
            }
        } else {
            utenti = utenteRepository.findAll();
        }
        return utenti.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<UtenteDTO> cercaUtenti(String role, String search) {
        if (role != null && !role.isBlank()) {
            try {
                return utenteRepository.findByRole(Utente.Ruolo.valueOf(role.toUpperCase())).stream().map(this::convertToDTO).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }
        if (search != null && !search.isBlank()) {
            return utenteRepository.cercaPerNomeCognomeOUsername(search).stream().map(this::convertToDTO).collect(Collectors.toList());
        }
        return utenteRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public UtenteDTO creaUtente(UtenteDTO dto) {
        Utente utente = convertToEntity(dto);
        if(utente.getRole() == null) utente.setRole("UTENTE_BASE");
        if(utente.getCredito() == null) utente.setCredito(0.0);
        if(utente.isAttivo() == null) utente.setAttivo(true);

        if (dto.getPassword() != null) {
            utente.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        Utente salvato = utenteRepository.save(utente);
        return convertToDTO(salvato);
    }

    public Optional<UtenteDTO> aggiornaUtente(Long id, UtenteDTO dto) {
        return utenteRepository.findById(id).map(esistente -> {
            if (dto.getNome() != null) esistente.setNome(dto.getNome());
            if (dto.getCognome() != null) esistente.setCognome(dto.getCognome());
            if (dto.getEmail() != null) esistente.setEmail(dto.getEmail());
            if (dto.getAttivo() != null) esistente.setAttivo(dto.getAttivo());
            if (dto.getPassword() != null) {
                esistente.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            
            Utente salvato = utenteRepository.save(esistente);
            return convertToDTO(salvato);
        });
    }

    public boolean eliminaUtente(Long id) {
        if(utenteRepository.existsById(id)) {
            utenteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean toggleUserStatus(String username) {
        return utenteRepository.findByUsername(username).map(utente -> {
            utente.setAttivo(!utente.isAttivo());
            utenteRepository.save(utente);
            return true;
        }).orElse(false);
    }

    public Optional<String> toggleUserRole(String username) {
        return utenteRepository.findByUsername(username).map(utente -> {
            if ("ADMIN".equalsIgnoreCase(utente.getRole())) {
                return "ADMIN_ROLE_PROTECTED";
            }
            String nuovoRuolo = "GESTORE".equalsIgnoreCase(utente.getRole()) ? "UTENTE_BASE" : "GESTORE";
            utente.setRole(nuovoRuolo);
            utenteRepository.save(utente);
            return "SUCCESS";
        });
    }

    public UtenteDTO convertToDTO(Utente utente) {
        UtenteDTO dto = new UtenteDTO();
        dto.setId(utente.getId());
        dto.setUsername(utente.getUsername());
        dto.setRole(utente.getRole());
        dto.setNome(utente.getNome());
        dto.setCognome(utente.getCognome());
        dto.setAnni(utente.getAnni());
        dto.setEmail(utente.getEmail());
        dto.setCredito(utente.getCredito());
        dto.setAttivo(utente.isAttivo());
        return dto;
    }

    public Optional<Utente> findByUsername(String username) {
        return utenteRepository.findByUsername(username);
    }

    private Utente convertToEntity(UtenteDTO dto) {
        Utente utente = new Utente();
        utente.setId(dto.getId());
        utente.setUsername(dto.getUsername());
        utente.setRole(dto.getRole());
        utente.setNome(dto.getNome());
        utente.setCognome(dto.getCognome());
        utente.setAnni(dto.getAnni());
        utente.setEmail(dto.getEmail());
        utente.setCredito(dto.getCredito());
        utente.setAttivo(dto.getAttivo());
        return utente;
    }
}

