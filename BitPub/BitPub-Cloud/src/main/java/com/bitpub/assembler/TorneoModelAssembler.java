package com.bitpub.assembler;

import com.bitpub.controllers.TorneoController;
import com.bitpub.dto.TorneoDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Componente architetturale Spring responsabile dell'assemblaggio delle rappresentazioni HATEOAS.
 * Svolge la funzione di traduttore tra il Data Transfer Object (DTO) e il corrispondente
 * modello ipertestuale (EntityModel) esposto dall'API REST [cite:23].
 * Disaccoppiando l'entità del database dalla logica di rete, previene il leakage
 * di informazioni sensibili e assicura l'incapsulamento del layer di persistenza.
 *
 * @author Stefano Bellan 20054330
 */
@Component
public class TorneoModelAssembler implements RepresentationModelAssembler<TorneoDTO, EntityModel<TorneoDTO>> {

    /**
     * Impacchetta il DTO all'interno di un EntityModel e vi inietta dinamicamente i collegamenti [cite:23].
     * L'algoritmo valuta a runtime lo stato logico del dominio (es. date di validità)
     * per stabilire quali azioni rendere disponibili al client consumatore.
     *
     * @param torneoDto L'istanza DTO immutabile contenente le informazioni di business
     * @return Una rappresentazione HATEOAS navigabile pronta per la serializzazione JSON
     */
    @Override
    public EntityModel<TorneoDTO> toModel(TorneoDTO torneoDto) {
        Long id = torneoDto.getId();

        // Istanziazione del contenitore HATEOAS con binding automatico ai metodi del controller Spring MVC
        EntityModel<TorneoDTO> torneoModel = EntityModel.of(torneoDto,
                linkTo(methodOn(TorneoController.class).getTorneoById(id)).withSelfRel(),
                linkTo(methodOn(TorneoController.class).aggiornaTorneo(id, null)).withRel("aggiorna_torneo"),
                linkTo(methodOn(TorneoController.class).eliminaTorneo(id)).withRel("elimina_torneo")
        );

        /*
         * REGOLA DI BUSINESS: HATEOAS CONDIZIONALE
         * La direttiva logica vincola l'esposizione del link per il "prossimo match" unicamente
         * ai tornei in corso di svolgimento o programmati per il futuro.
         * I tornei storicizzati (conclusi) non riceveranno l'azione nel payload finale.
         */
        if (torneoDto.getDataFine() == null || torneoDto.getDataFine().isAfter(LocalDate.now())) {
            torneoModel.add(linkTo(methodOn(TorneoController.class).getProssimoMatch(id)).withRel("nextMatch"));
        }

        return torneoModel;
    }
}