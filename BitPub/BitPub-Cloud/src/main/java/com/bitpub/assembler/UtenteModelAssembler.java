package com.bitpub.assembler;

import com.bitpub.controllers.UtenteController;
import com.bitpub.dto.UtenteDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UtenteModelAssembler implements RepresentationModelAssembler<UtenteDTO, EntityModel<UtenteDTO>> {

    @Override
    public EntityModel<UtenteDTO> toModel(UtenteDTO utenteDto) {
        EntityModel<UtenteDTO> utenteModel = EntityModel.of(utenteDto,
                linkTo(methodOn(UtenteController.class).getUtenteById(utenteDto.getId())).withSelfRel(),
                linkTo(methodOn(UtenteController.class).getAllUtenti(null)).withRel("tutti_utenti"),
                linkTo(methodOn(UtenteController.class).aggiornaUtente(utenteDto.getId(), null)).withRel("aggiorna_profilo")
        );

        // Conditional links based on role
        if ("GESTORE".equals(utenteDto.getRole())) {
            // Future feature: link to gestore dashboard / actions
        }

        return utenteModel;
    }
}
