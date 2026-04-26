package com.bitpub.assembler;

import com.bitpub.controllers.TorneoController;
import com.bitpub.models.Torneo;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Componente responsabile della trasformazione di istanze {@link Torneo} in {@link EntityModel}.
 * <p>
 * Implementa il pattern HATEOAS, arricchendo il modello con link ipertestuali 
 * che guidano il client nelle possibili interazioni con la risorsa Torneo.
 * </p>
 * 
 * @author Stefano Bellan 20054330
 */
@Component
public class TorneoModelAssembler implements RepresentationModelAssembler<Torneo, EntityModel<Torneo>> {

    /**
     * Converte l'entità Torneo in un modello rappresentativo arricchito di link.
     * <p>
     * Oltre ai link statici (self, aggiorna, elimina), il metodo applica una logica condizionale:
     * il link "nextMatch" viene esposto solo se il torneo non è ancora concluso.
     * </p>
     *
     * @param torneo L'entità di dominio da convertire.
     * @return L'EntityModel contenente i dati del torneo e i relativi link HATEOAS.
     */
    @Override
    public EntityModel<Torneo> toModel(Torneo torneo) {
        Long id = torneo.getId();

        // Creazione del modello base con i link di gestione risorsa (CRUD)
        EntityModel<Torneo> torneoModel = EntityModel.of(torneo,
                linkTo(methodOn(TorneoController.class).getTorneoById(id)).withSelfRel(),
                linkTo(methodOn(TorneoController.class).aggiornaTorneo(id, null, null)).withRel("aggiorna_torneo"),
                linkTo(methodOn(TorneoController.class).eliminaTorneo(id, null)).withRel("elimina_torneo")
        );

        /* 
         * Logica HATEOAS Condizionale: 
         * Se il torneo è in corso o futuro (dataFine nulla o non ancora passata), 
         * aggiungo l'endpoint per recuperare il prossimo match.
         */
        if (torneo.getDataFine() == null || torneo.getDataFine().isAfter(LocalDate.now().minusDays(1))) {
            torneoModel.add(
                linkTo(methodOn(TorneoController.class).getProssimaPartita(id)).withRel("nextMatch")
            );
        }
        
        return torneoModel;
    }
}
