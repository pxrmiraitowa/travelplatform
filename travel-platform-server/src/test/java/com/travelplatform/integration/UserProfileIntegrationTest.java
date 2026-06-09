package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class UserProfileIntegrationTest extends AbstractIntegrationTest {

    @Test
    void currentUserProfileShouldSupportQueryAndUpdate() throws Exception {
        AuthSession session = registerUserAndLogin();

        JsonNode currentUserRoot = performJson(
                get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(currentUserRoot);
        assertThat(currentUserRoot.path("data").path("username").asText()).isEqualTo(session.username());

        JsonNode updateRoot = performJson(
                put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "nickname", "Updated Integration User",
                                "realName", "Test User",
                                "phone", "13900000000",
                                "email", "integration@example.com",
                                "gender", 1,
                                "avatar", "/uploads/avatar.png"
                        )))
        );
        assertSuccess(updateRoot);
        assertThat(updateRoot.path("data").path("nickname").asText()).isEqualTo("Updated Integration User");
        assertThat(updateRoot.path("data").path("phone").asText()).isEqualTo("13900000000");
    }
}
