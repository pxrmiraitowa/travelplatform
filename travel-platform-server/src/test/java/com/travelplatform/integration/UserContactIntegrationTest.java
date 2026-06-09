package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class UserContactIntegrationTest extends AbstractIntegrationTest {

    @Test
    void userContactsShouldSupportCrudFlow() throws Exception {
        AuthSession session = registerUserAndLogin();
        Long contactId = createContact(session.token(), "First Contact");

        JsonNode listRoot = performJson(
                get("/api/user-contacts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(listRoot);
        assertThat(listRoot.path("data").isArray()).isTrue();
        assertThat(listRoot.path("data").size()).isEqualTo(1);

        JsonNode updateRoot = performJson(
                put("/api/user-contacts/{id}", contactId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "name", "Updated Contact",
                                "phone", "13812345678",
                                "idCard", "310101199201011234",
                                "contactType", 2,
                                "isDefault", 0,
                                "remark", "updated"
                        )))
        );
        assertSuccess(updateRoot);
        assertThat(updateRoot.path("data").path("name").asText()).isEqualTo("Updated Contact");

        JsonNode deleteRoot = performJson(
                delete("/api/user-contacts/{id}", contactId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(deleteRoot);

        JsonNode finalListRoot = performJson(
                get("/api/user-contacts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(finalListRoot);
        assertThat(finalListRoot.path("data").size()).isZero();
    }
}
