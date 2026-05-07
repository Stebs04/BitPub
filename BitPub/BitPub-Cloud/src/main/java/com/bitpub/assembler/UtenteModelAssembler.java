package com.bitpub.assembler;

import com.bitpub.controllers.UtenteController;
import com.bitpub.dto.UtenteDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller di assemblaggio HATEOAS dedicato all'impacchettamento del dominio Utente
 * A seguito del refactoring architetturale, opera esclusivamente sul Data Transfer Object (UtenteDTO),
 * isolando completamente l'API REST dall'Entity JPA sottostante. Questa separazione
 * previene alla radice il leakage di campi riservati (come l'hash della password o i
 * metadati di persistenza) durante le fasi di serializzazione JSON.
 *
 * @author Stefano Bellan 20054330
 */
@Component
public class UtenteModelAssembler implements RepresentationModelAssembler<UtenteDTO, EntityModel<UtenteDTO>> {

    /**
     * Mappa i dati autorizzati dell'utente e li incapsula in un EntityModel [cite:23].
     * Arricchisce dinamicamente l'involucro con direttive ipertestuali relative
     * sia ad azioni amministrative sia alla scoperta di risorse satellite.
     *
     * @param utenteDto Il DTO asettico originato dal layer dei servizi
     * @return Il wrapper navigabile strutturato secondo gli standard Spring HATEOAS
     */
    @Override
    public EntityModel<UtenteDTO> toModel(UtenteDTO utenteDto) {
        // Estrazione delle chiavi di routing (Id per azioni dirette, Nickname per URI SEO-friendly)
        String nick = utenteDto.getUsername();
        Long id = utenteDto.getId();

        /*
         * REGISTRAZIONE DELL'ALBERO IPERMEDIALE
         * Inietta le direttive operative e di discovery sfruttando la riflessione metodologica
         * di Spring MVC, garantendo che gli URL si adeguino automaticamente ad eventuali
         * modifiche delle rotte nel controller.
         */
        return EntityModel.of(utenteDto,
                // Risoluzione canonica (Self-Relation) puntata alla radice pubblica del profilo
                linkTo(methodOn(UtenteController.class).getUtenteByNickname(nick, null)).withSelfRel(),

                // Indirizzo logico riservato alle operazioni CRUD di mutazione dello stato
                linkTo(methodOn(UtenteController.class).aggiornaUtente(id, null)).withRel("modifica"),

                // Puntatori di aggregazione verso i sottomoduli del dominio utente
                linkTo(methodOn(UtenteController.class).getPartiteUtente(nick)).withRel("partite"),
                linkTo(methodOn(UtenteController.class).getStatisticheUtente(nick)).withRel("dashboard_statistiche")
        );
    }
}