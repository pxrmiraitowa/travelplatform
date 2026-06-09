package com.travelplatform.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

final class ValidationTestUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private ValidationTestUtils() {
    }

    static <T> Set<String> validateProperty(T target, String propertyName) {
        return VALIDATOR.validateProperty(target, propertyName)
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    static <T> Set<String> validate(T target) {
        return VALIDATOR.validate(target)
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
