package com.bitpub.services;

import com.bitpub.dto.PartitaCalciobalillaDTO;
import com.bitpub.models.CalciobalillaStats;
import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CalciobalillaService {

    @Autowired
    private PartitaCalciobalillaRepository repository;

    public List<PartitaCalciobalillaDTO> getAllPartite() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<PartitaCalciobalillaDTO> getPartitaById(Long id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    public CalciobalillaStats getGlobalStats() {
        int rullate = repository.findAll().stream()
                .mapToInt(PartitaCalciobalilla::getTotaleRullate)
                .sum();
        int vRossi = repository.countVittorieRossi();
        int vBlu = repository.countVittorieBlu();
        return new CalciobalillaStats(rullate, vRossi, vBlu);
    }

    private PartitaCalciobalillaDTO convertToDTO(PartitaCalciobalilla entity) {
        PartitaCalciobalillaDTO dto = new PartitaCalciobalillaDTO();
        dto.setId(entity.getId());
        dto.setNomeSquadraRossa(entity.getNomeSquadraRossa());
        dto.setNomeSquadraBlu(entity.getNomeSquadraBlu());
        dto.setPunteggioRossi(entity.getPunteggioRossi());
        dto.setPunteggioBlu(entity.getPunteggioBlu());
        dto.setSqualificheRossi(entity.getSqualificheRossi());
        dto.setSqualificheBlu(entity.getSqualificheBlu());
        dto.setTotaleRullate(entity.getTotaleRullate());
        dto.setDataFine(entity.getDataFine());
        if (entity.getTorneo() != null) {
            dto.setTorneoId(entity.getTorneo().getId());
        }
        return dto;
    }
}
