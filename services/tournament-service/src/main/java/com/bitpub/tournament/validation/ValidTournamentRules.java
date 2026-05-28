package com.bitpub.tournament.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TournamentRulesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTournamentRules {
    String message() default "Regole del torneo non valide";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
