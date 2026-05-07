package com.bitpub.assembler;

import com.bitpub.controllers.LocaleController;
import com.bitpub.dto.LocaleDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Componente d'assemblaggio HATEOAS preposto all'incapsulamento della risorsa Locale
 * Disaccoppiando l'Entity fisica attraverso l'utilizzo di LocaleDTO, la classe espone un payload
 * ripulito e arricchito con marcatori di navigazione. Questi puntatori ipermediali sollevano
 * i client dall'onere di conoscere a priori la mappatura delle dipendenze (es. i dispositivi
 * installati in sede), promuovendo un'architettura API autodescrittiva e altamente navigabile.
 *
 * @author Stefano Bellan 20054330
 */
@Component
public class LocaleModelAssembler implements RepresentationModelAssembler<LocaleDTO, EntityModel<LocaleDTO>> {

    /**
     * Applica la trasformazione dal Data Transfer Object a un nodo HATEOAS navigabile [cite:23].
     * Anziché esporre un oggetto statico, fornisce alla controparte un grafo relazionale
     * pronto per la discovery automatica delle risorse figlie.
     *
     * @param localeDto La rappresentazione decontestualizzata della sede fisica
     * @return Una struttura Spring EntityModel confezionata con i link di riferimento
     */
    @Override
    public EntityModel<LocaleDTO> toModel(LocaleDTO localeDto) {
        Long id = localeDto.getId();

        /*
         * COSTRUZIONE DELL'INVOLUCRO IPERTESTUALE
         * Utilizza la factory statica WebMvcLinkBuilder per mappare retrospettivamente
         * i metodi del controller sui percorsi fisici generati dal dispatcher HTTP.
         */
        return EntityModel.of(localeDto,
                // Relazione identitaria (Self) per ricaricare la singola istanza
                linkTo(methodOn(LocaleController.class).getLocaleById(id)).withSelfRel(),

                // Relazione di scoperta (Discovery) per l'attraversamento della collezione
                // One-To-Many, guidando i client verso la mappa hardware censita in questa sede
                linkTo(methodOn(LocaleController.class).getDispositiviLocale(id)).withRel("dispositivi")
        );
    }
}