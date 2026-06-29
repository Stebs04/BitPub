package com.bitpub.services;

import com.bitpub.dto.TorneoDTO;
import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TorneoService {

    @Autowired
    private TorneoRepository torneoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    public Optional<TorneoDTO> getTorneoById(Long id) {
        return torneoRepository.findById(id).map(this::convertToDTO);
    }

    public List<TorneoDTO> getAllTornei() {
        return torneoRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TorneoDTO creaTorneo(TorneoDTO torneoDTO) {
        Torneo torneo = convertToEntity(torneoDTO);
        Torneo salvato = torneoRepository.save(torneo);
        return convertToDTO(salvato);
    }

    public Optional<TorneoDTO> aggiornaTorneo(Long id, TorneoDTO datiAggiornati) {
        return torneoRepository.findById(id).map(torneo -> {
            if (datiAggiornati.getNome() != null) {
                torneo.setNome(datiAggiornati.getNome());
            }
            if (datiAggiornati.getPremio() != null) {
                torneo.setPremio(datiAggiornati.getPremio());
            }
            Torneo salvato = torneoRepository.save(torneo);
            return convertToDTO(salvato);
        });
    }

    public boolean eliminaTorneo(Long id) {
        if (torneoRepository.existsById(id)) {
            torneoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean iscriviUtente(Long torneoId, Long utenteId) {
        // Logic for subscribing user (simplified as it was in the controller, it just checked if present)
        return torneoRepository.existsById(torneoId) && utenteRepository.existsById(utenteId);
    }

    private TorneoDTO convertToDTO(Torneo torneo) {
        TorneoDTO dto = new TorneoDTO();
        dto.setId(torneo.getId());
        dto.setNome(torneo.getNome());
        dto.setTipoGioco(torneo.getTipoGioco() != null ? torneo.getTipoGioco().name() : null);
        dto.setLocaleId(torneo.getLocaleId());
        dto.setDataInizio(torneo.getDataInizio());
        dto.setDataFine(torneo.getDataFine());
        dto.setPremio(torneo.getPremio());
        dto.setMaxPartecipanti(torneo.getMaxPartecipanti());
        dto.setModalita(torneo.getModalita() != null ? torneo.getModalita().name() : null);
        return dto;
    }

    private Torneo convertToEntity(TorneoDTO dto) {
        Torneo torneo = new Torneo();
        torneo.setId(dto.getId());
        torneo.setNome(dto.getNome());
        if (dto.getTipoGioco() != null) {
            torneo.setTipoGioco(Torneo.TipoGioco.valueOf(dto.getTipoGioco()));
        }
        torneo.setLocaleId(dto.getLocaleId());
        torneo.setDataInizio(dto.getDataInizio());
        torneo.setDataFine(dto.getDataFine());
        torneo.setPremio(dto.getPremio());
        torneo.setMaxPartecipanti(dto.getMaxPartecipanti());
        if (dto.getModalita() != null) {
            torneo.setModalita(Torneo.ModalitaTorneo.valueOf(dto.getModalita()));
        }
        return torneo;
    }
}
