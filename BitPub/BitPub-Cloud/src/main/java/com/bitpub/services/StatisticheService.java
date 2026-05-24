package com.bitpub.services;

import com.bitpub.dto.StatisticheLocaleDTO;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticheService {

    private final PartitaCalciobalillaRepository calciobalillaRepository;

    @Autowired
    public StatisticheService(PartitaCalciobalillaRepository calciobalillaRepository){
        this.calciobalillaRepository = calciobalillaRepository;
    }

    public StatisticheLocaleDTO calcolaStatisticheCalciobalilla(Long localeId)  {
        StatisticheLocaleDTO stats = new StatisticheLocaleDTO();
        stats.setLocaleId(localeId);

        int vittorieRossi = calciobalillaRepository.countVittorieRossiByLocale(localeId);
        int vittorieBlu = calciobalillaRepository.countVittorieBluByLocale(localeId);
        int partiteTotali = vittorieRossi + vittorieBlu;

        //Calcolo percentuali
        if(partiteTotali > 0) {
            stats.setPercentualeVittorieRossi(Math.round(((double) vittorieRossi / partiteTotali) * 100.0 * 100.0) / 100.0);
            stats.setPercentualeVittorieBlu(Math.round(((double) vittorieBlu / partiteTotali) * 100.0 * 100.0) / 100.0);
        } else {
            stats.setPercentualeVittorieRossi(0.0);
            stats.setPercentualeVittorieBlu(0.0);
        }

        //Recupero Medie
        Double durataMedia = calciobalillaRepository.calculateAverageDuration(localeId);
        stats.setDurataMediaMinuti(durataMedia != null ? Math.round(durataMedia * 100.0) / 100.0 : 0.0);

        stats.setTotaleRullate(calciobalillaRepository.countRullateByLocale(localeId));

        return stats;
    }
}
