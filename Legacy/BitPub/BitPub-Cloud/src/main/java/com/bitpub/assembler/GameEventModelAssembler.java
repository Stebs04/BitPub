package com.bitpub.assembler;

import com.bitpub.controllers.BiliardoController;
import com.bitpub.controllers.CalciobalillaController;
// import com.bitpub.controllers.FreccetteController; // Pronto per essere decommentato in futuro
import com.bitpub.dto.GameEventDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * GameEventModelAssembler - Trasforma il DTO in una risorsa HATEOAS.
 *
 * Refactoring Senior Note:
 * Risolto il problema del "Polymorphic Linking". 
 * Essendo questo assembler ora condiviso tra più giochi (Biliardo, Calciobalilla, ecc.),
 * non possiamo hardcodare 'BiliardoController'. La generazione dei link viene ora
 * effettuata dinamicamente ispezionando l'origine dell'evento (tableId),
 * garantendo che ogni DTO punti al proprio endpoint REST di competenza 
 * disaccoppiando del tutto i controller.
 */
@Component
public class GameEventModelAssembler implements RepresentationModelAssembler<GameEventDTO, EntityModel<GameEventDTO>> {

    @Override
    public EntityModel<GameEventDTO> toModel(GameEventDTO event) {
        // Risoluzione dinamica del controller corretto in base all'ID del tavolo
        Class<?> targetController = determineController(event.getTableId());

        // Usiamo linkTo(Class).slash() per generare i link HATEOAS dinamicamente
        // Questa pratica permette di bypassare il limite di methodOn() che vincola a un tipo statico.
        return EntityModel.of(event,
                linkTo(targetController).slash("event").slash(event.getEventId()).withSelfRel(),
                linkTo(targetController).slash("session").slash(event.getSessionId()).slash("events").withRel("session_events"));
    }

    /**
     * Determina a quale Controller REST appartiene l'evento ispezionando il tableId.
     * Assume la naming convention standard di BitPub (es. "BIL-1", "CAL-1", "FRE-1").
     *
     * @param tableId Identificativo del tavolo (es. CAL-01)
     * @return La classe del controller di competenza
     */
    private Class<?> determineController(String tableId) {
        if (tableId != null) {
            String id = tableId.toUpperCase();
            
            if (id.startsWith("CAL")) {
                return CalciobalillaController.class;
            }
            
            // Predisposizione architetturale per le Freccette (Open/Closed Principle)
            // if (id.startsWith("FRE")) {
            //     return FreccetteController.class;
            // }
        }
        
        // Fallback predefinito
        return BiliardoController.class;
    }
}