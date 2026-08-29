package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class PriceAlertIntegrationTest extends AbstractIntegrationTest {

    @Test
    void priceAlertShouldPersistListRejectDuplicateAndDeleteOnlyOwnedAlert() throws Exception {
        AuthSession owner = registerUserAndLogin();
        Long flightId = findFutureFlightId();
        BigDecimal targetPrice = new BigDecimal("100.00");

        JsonNode created = createAlert(owner.token(), flightId, targetPrice);
        assertSuccess(created);
        Long alertId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("productType").asText()).isEqualTo("FLIGHT");
        assertThat(created.path("data").path("productId").asLong()).isEqualTo(flightId);
        assertThat(created.path("data").path("targetPrice").decimalValue()).isEqualByComparingTo(targetPrice);
        assertThat(created.path("data").path("productName").asText()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from price_alert where id = ? and user_id = ? and status = 1",
                Integer.class, alertId, owner.userId())).isEqualTo(1);

        JsonNode listed = performJson(get("/api/price-alerts")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())));
        assertSuccess(listed);
        assertThat(listed.path("data").size()).isEqualTo(1);
        assertThat(listed.path("data").get(0).path("id").asLong()).isEqualTo(alertId);

        JsonNode duplicate = createAlert(owner.token(), flightId, targetPrice);
        assertFailure(duplicate, 400);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from price_alert where product_type = 'FLIGHT' and product_id = ? and user_id = ? and status = 1",
                Integer.class, flightId, owner.userId())).isEqualTo(1);

        AuthSession anotherUser = registerSecondUserAndLogin();
        JsonNode anotherAlert = createAlert(anotherUser.token(), flightId, targetPrice);
        assertSuccess(anotherAlert);
        Long anotherAlertId = anotherAlert.path("data").path("id").asLong();

        JsonNode forbiddenDelete = performJson(delete("/api/price-alerts/{id}", anotherAlertId)
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())));
        assertFailure(forbiddenDelete, 404);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from price_alert where id = ? and user_id = ? and status = 1",
                Integer.class, anotherAlertId, anotherUser.userId())).isEqualTo(1);

        JsonNode deleted = performJson(delete("/api/price-alerts/{id}", alertId)
                .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())));
        assertSuccess(deleted);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from price_alert where id = ? and user_id = ?",
                Integer.class, alertId, owner.userId())).isZero();
    }

    private JsonNode createAlert(String token, Long flightId, BigDecimal targetPrice) throws Exception {
        return performJson(post("/api/price-alerts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "productType", "flight",
                        "productId", flightId,
                        "targetPrice", targetPrice,
                        "remark", "integration price watch"
                ))));
    }

    private AuthSession registerSecondUserAndLogin() throws Exception {
        String suffix = uniqueSuffix();
        String username = "iu2_" + suffix.substring(Math.max(0, suffix.length() - 14));
        String phone = "19" + suffix.substring(Math.max(0, suffix.length() - 9));
        JsonNode root = performJson(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "username", username,
                        "nickname", "Second Integration User",
                        "phone", phone,
                        "password", "123456",
                        "confirmPassword", "123456"
                ))));
        assertSuccess(root);
        return new AuthSession(
                root.path("data").path("token").asText(),
                root.path("data").path("userInfo").path("id").asLong(),
                username,
                phone
        );
    }
}
