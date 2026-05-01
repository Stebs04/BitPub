package com.bitpub.assembler;

import com.bitpub.controllers.UtenteController;
import com.bitpub.models.Utente;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Componente per la trasformazione ipertestuale dell'entità {@link Utente}.
 * <p>
 * Gestisce la mappatura dei link ipertestuali relativi al profilo utente, 
 * inclusi i punti di accesso alle statistiche e allo storico partite.
 * </p>
 * @author Stefano Bellan 20054330
 */
@Component
public class UtenteModelAssembler implements RepresentationModelAssembler<Utente, EntityModel<Utente>> {

    /**
     * Arricchisce l'entità Utente con i link HATEOAS necessari alla navigazione del profilo.
     * <p>
     * Nota: Viene utilizzato il {@code nickname} come chiave naturale per la risoluzione 
     * dei percorsi, garantendo URL più leggibili (SEO-friendly/User-friendly).
     * </p>
     *
     * @param utente L'oggetto di dominio da mappare.
     * @return {@link EntityModel} contenente i dati dell'utente e i link relazionali.
     */
    @Override
    public EntityModel<Utente> toModel(Utente utente) {
        // Estrazione della chiave naturale per la costruzione degli endpoint
        String nick = utente.getNickname();

        /*
         * Definizione dei link ipertestuali:
         * - self: Accesso al profilo tramite nickname.
         * - modifica: Operazioni di aggiornamento (Action link).
         * - partite: Lista degli eventi a cui l'utente ha partecipato.
         * - dashboard_statistiche: Endpoint aggregatore per metriche di performance.
         */
        return EntityModel.of(utente,
                linkTo(methodOn(UtenteController.class).getUtenteByNickname(nick, null)).withSelfRel(),
                
                // Endpoint di mutazione risorsa
                linkTo(methodOn(UtenteController.class).modificaUtente(nick, null)).withRel("modifica"),
                
                // Collegamenti a risorse correlate (Discovery)
                linkTo(methodOn(UtenteController.class).getPartiteUtente(nick)).withRel("partite"),
                linkTo(methodOn(UtenteController.class).getStatisticheUtente(nick)).withRel("dashboard_statistiche")
        );
    }
}
