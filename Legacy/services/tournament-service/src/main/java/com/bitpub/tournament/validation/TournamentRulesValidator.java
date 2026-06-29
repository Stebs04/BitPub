package com.bitpub.tournament.validation;

import com.bitpub.tournament.dto.CreateTournamentRequest;
import com.bitpub.tournament.model.TournamentFormat;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class TournamentRulesValidator implements ConstraintValidator<ValidTournamentRules, CreateTournamentRequest> {

    @Override
    public boolean isValid(CreateTournamentRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Use @NotNull at class level if needed, usually validated fields do null checks individually
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // 1. Team size validation based on format
        if (request.getFormat() == TournamentFormat.SINGLE_ELIMINATION && request.getTeamSize() < 1) {
            context.buildConstraintViolationWithTemplate("La dimensione del team deve essere almeno 1")
                   .addPropertyNode("teamSize")
                   .addConstraintViolation();
            valid = false;
        }

        // 2. Start date must be in the future (at least 1 hour from now)
        if (request.getStartDate() != null && request.getStartDate().isBefore(LocalDateTime.now().plusHours(1))) {
            context.buildConstraintViolationWithTemplate("La data di inizio deve essere almeno 1 ora nel futuro")
                   .addPropertyNode("startDate")
                   .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
