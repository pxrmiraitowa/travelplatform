package com.travelplatform.dto;

import com.travelplatform.dto.usercontact.UserContactCreateRequest;
import com.travelplatform.dto.usercontact.UserContactUpdateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContactRequestValidationTest {

    @Test
    void shouldAcceptValidPhoneNumberWhenCreatingContact() {
        UserContactCreateRequest request = buildValidCreateRequest();
        request.setPhone("14452867538");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isEmpty();
    }

    @Test
    void shouldRejectInvalidPhoneNumberWhenCreatingContact() {
        UserContactCreateRequest request = buildValidCreateRequest();
        request.setPhone("1445286753");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isNotEmpty();
    }

    @Test
    void shouldAcceptValidPhoneNumberWhenUpdatingContact() {
        UserContactUpdateRequest request = buildValidUpdateRequest();
        request.setPhone("14452867538");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isEmpty();
    }

    @Test
    void shouldRejectInvalidPhoneNumberWhenUpdatingContact() {
        UserContactUpdateRequest request = buildValidUpdateRequest();
        request.setPhone("1445286753");

        assertThat(ValidationTestUtils.validateProperty(request, "phone")).isNotEmpty();
    }

    private UserContactCreateRequest buildValidCreateRequest() {
        UserContactCreateRequest request = new UserContactCreateRequest();
        request.setName("Tester");
        request.setPhone("13900000000");
        request.setIdCard("110101199001011234");
        return request;
    }

    private UserContactUpdateRequest buildValidUpdateRequest() {
        UserContactUpdateRequest request = new UserContactUpdateRequest();
        request.setName("Tester");
        request.setPhone("13900000000");
        request.setIdCard("110101199001011234");
        return request;
    }
}
