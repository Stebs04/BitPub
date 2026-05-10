package com.bitpub.services;

import com.bitpub.dto.LocaleDTO;
import com.bitpub.models.Locale;
import com.bitpub.repository.LocaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocaleService {

    @Autowired
    private LocaleRepository localeRepository;

    public List<LocaleDTO> getAllLocali() {
        return localeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<LocaleDTO> getLocaleById(Long id) {
        return localeRepository.findById(id).map(this::convertToDTO);
    }

    public LocaleDTO creaLocale(LocaleDTO dto) {
        Locale entity = convertToEntity(dto);
        Locale salvato = localeRepository.save(entity);
        return convertToDTO(salvato);
    }

    public Optional<LocaleDTO> aggiornaLocale(Long id, LocaleDTO dto) {
        return localeRepository.findById(id).map(esistente -> {
            if (dto.getNome() != null) esistente.setName(dto.getNome());
            if (dto.getIpAddressEdge() != null) esistente.setIpAddressEdge(dto.getIpAddressEdge());
            if (dto.getIndirizzo() != null) esistente.setIndirizzo(dto.getIndirizzo());
            if (dto.getCitta() != null) esistente.setCitta(dto.getCitta());
            if (dto.getCapienza() != null) esistente.setCapienza(dto.getCapienza());
            if (dto.getGestoreId() != null) esistente.setGestoreId(dto.getGestoreId());
            
            Locale salvato = localeRepository.save(esistente);
            return convertToDTO(salvato);
        });
    }

    public boolean eliminaLocale(Long id) {
        if (localeRepository.existsById(id)) {
            localeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<LocaleDTO> getLocaliByGestoreId(Long gestoreId) {
        return localeRepository.findByGestoreId(gestoreId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LocaleDTO convertToDTO(Locale entity) {
        LocaleDTO dto = new LocaleDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getName());
        dto.setIpAddressEdge(entity.getIpAddressEdge());
        dto.setIndirizzo(entity.getIndirizzo());
        dto.setCitta(entity.getCitta());
        dto.setCapienza(entity.getCapienza());
        dto.setGestoreId(entity.getGestoreId());
        return dto;
    }

    private Locale convertToEntity(LocaleDTO dto) {
        Locale entity = new Locale();
        entity.setId(dto.getId());
        entity.setName(dto.getNome());
        entity.setIpAddressEdge(dto.getIpAddressEdge());
        entity.setIndirizzo(dto.getIndirizzo());
        entity.setCitta(dto.getCitta());
        entity.setCapienza(dto.getCapienza());
        entity.setGestoreId(dto.getGestoreId());
        return entity;
    }
}
