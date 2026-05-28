package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class EventValidator {

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Validates a BaseSensorEvent using Jakarta Bean Validation.
     *
     * @param event The event to validate.
     * @throws IllegalArgumentException if the event is invalid, with a message containing all violations.
     */
    public static void validate(BaseSensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Set<ConstraintViolation<BaseSensorEvent>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Event validation failed: " + errors);
        }
    }
}
