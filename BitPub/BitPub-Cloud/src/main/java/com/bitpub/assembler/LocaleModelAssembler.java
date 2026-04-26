package com.bitpub.assembler;

import com.bitpub.controllers.LocaleController;
import com.bitpub.models.Locale;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler dedicato alla risorsa {@link Locale}.
 * <p>
 * Trasforma il dominio {@link Locale} in un modello ipertestuale, 
 * facilitando la navigazione verso le risorse correlate, come i dispositivi 
 * associati al punto vendita o alla sala.
 * </p>
 * @author Stefano Bellan 20054330
 */
@Component
public class LocaleModelAssembler implements RepresentationModelAssembler<Locale, EntityModel<Locale>> {

    /**
     * Mappa l'entità Locale in un {@link EntityModel} arricchito.
     * <p>
     * Oltre al link di autodescrizione (self), viene fornito il link relazionale 
     * "dispositivi" per permettere al client di scoprire gli apparati hardware 
     * collegati a questo specifico locale.
     * </p>
     *
     * @param locale L'istanza dell'entità di dominio.
     * @return EntityModel contenente i dati e i link di navigazione.
     */
    @Override
    public EntityModel<Locale> toModel(Locale locale) {
        // Estrazione dell'identificativo per il binding dei link
        Long id = locale.getId();

        /*
         * Costruzione del modello:
         * - self: Punta al dettaglio della risorsa corrente.
         * - dispositivi: Punto di accesso alla sotto-risorsa (One-to-Many).
         */
        return EntityModel.of(locale,
                linkTo(methodOn(LocaleController.class).getById(id)).withSelfRel(),
                linkTo(methodOn(LocaleController.class).getDispositiviLocale(id)).withRel("dispositivi")
        );
    }
}
