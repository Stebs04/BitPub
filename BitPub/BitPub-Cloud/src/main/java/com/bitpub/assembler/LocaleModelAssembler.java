package com.bitpub.assembler;

import com.bitpub.controllers.LocaleController;
import com.bitpub.dto.LocaleDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class LocaleModelAssembler implements RepresentationModelAssembler<LocaleDTO, EntityModel<LocaleDTO>> {

    @Override
    public EntityModel<LocaleDTO> toModel(LocaleDTO localeDto) {
        Long id = localeDto.getId();

        return EntityModel.of(localeDto,
                linkTo(methodOn(LocaleController.class).getLocaleById(id)).withSelfRel(),
                linkTo(methodOn(LocaleController.class).getAllLocali()).withRel("tutti_locali"),
                linkTo(methodOn(LocaleController.class).aggiornaLocale(id, null)).withRel("aggiorna_locale"),
                linkTo(methodOn(LocaleController.class).eliminaLocale(id)).withRel("elimina_locale")
        );
    }
}
