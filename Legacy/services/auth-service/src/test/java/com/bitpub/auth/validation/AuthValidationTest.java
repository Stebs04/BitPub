package com.bitpub.auth.validation;

import com.bitpub.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testValidRegisterRequest() {
        RegisterRequest request = RegisterRequest.builder()
                .username("mario_rossi")
                .password("Secur3Pass!")
                .email("mario@example.com")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "La richiesta dovrebbe essere valida");
    }

    @Test
    void testInvalidRegisterRequest_EmptyUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("") // Vuoto
                .password("Secur3Pass!")
                .email("mario@example.com")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Dovrebbero esserci violazioni per username vuoto");
    }

    @Test
    void testInvalidRegisterRequest_InvalidEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("mario_rossi")
                .password("Secur3Pass!")
                .email("not-an-email") // Email non valida
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Dovrebbero esserci violazioni per email non valida");
    }

    @Test
    void testInvalidRegisterRequest_ShortPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .username("mario_rossi")
                .password("short") // Password troppo corta (min 8)
                .email("mario@example.com")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Dovrebbero esserci violazioni per password troppo corta");
    }
}
