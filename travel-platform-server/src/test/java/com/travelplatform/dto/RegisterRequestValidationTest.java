package com.travelplatform.dto;

import com.travelplatform.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    @Test
    void shouldAcceptValidPhoneNumber() {
        RegisterRequest request = buildValidRequest();
        request.setPhone("14452867538");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isEmpty();
    }

    @Test
    void shouldRejectPhoneNumberWhenLengthIsNotElevenDigits() {
        RegisterRequest request = buildValidRequest();
        request.setPhone("1445286753");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isNotEmpty();
    }

    @Test
    void shouldRejectPhoneNumberWhenItContainsNonDigits() {
        RegisterRequest request = buildValidRequest();
        request.setPhone("1445286753a");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isNotEmpty();
    }

    private RegisterRequest buildValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("demoUser");
        request.setNickname("demo");
        request.setPhone("13900000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        return request;
    }
}
