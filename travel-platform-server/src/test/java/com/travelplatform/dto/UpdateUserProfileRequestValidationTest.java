package com.travelplatform.dto;

import com.travelplatform.dto.user.UpdateUserProfileRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateUserProfileRequestValidationTest {

    @Test
    void shouldAllowBlankPhoneNumberForProfileUpdate() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhone("");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isEmpty();
    }

    @Test
    void shouldAllowValidPhoneNumberForProfileUpdate() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhone("14452867538");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isEmpty();
    }

    @Test
    void shouldRejectInvalidPhoneNumberForProfileUpdate() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhone("1445286753");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isNotEmpty();
    }
}
