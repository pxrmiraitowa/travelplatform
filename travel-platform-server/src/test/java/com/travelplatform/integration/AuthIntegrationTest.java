package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAndLogoutShouldWorkEndToEnd() throws Exception {
        AuthSession session = registerUserAndLogin();

        JsonNode loginRoot = performJson(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "username", session.username(),
                                "password", "123456"
                        )))
        );
        assertSuccess(loginRoot);
        assertThat(loginRoot.path("data").path("token").asText()).isNotBlank();

        JsonNode logoutRoot = performJson(
                post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(logoutRoot);
    }
}
