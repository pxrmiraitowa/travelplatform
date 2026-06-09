package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.constant.OrderStatusConstant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ReviewShareAdminIntegrationTest extends AbstractIntegrationTest {

    @Test
    void reviewShareAndAdminManagementFlowsShouldWorkTogether() throws Exception {
        AuthSession userSession = registerUserAndLogin();
        AuthSession adminSession = ensureAdminAndLogin();
        Long contactId = createContact(userSession.token(), "Review Passenger");

        JsonNode orderRoot = performJson(
                post("/api/orders/flights")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "flightId", findFutureFlightId(),
                                "contactId", contactId,
                                "remark", "review-ready order"
                        )))
        );
        assertSuccess(orderRoot);
        Long orderId = orderRoot.path("data").path("id").asLong();

        JsonNode adminMeRoot = performJson(
                get("/api/admin/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(adminMeRoot);
        assertThat(adminMeRoot.path("data").path("username").asText()).isEqualTo(adminSession.username());

        JsonNode dashboardRoot = performJson(
                get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(dashboardRoot);

        JsonNode rolesRoot = performJson(
                get("/api/admin/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(rolesRoot);
        assertThat(rolesRoot.path("data").isArray()).isTrue();

        JsonNode userListRoot = performJson(
                get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .param("keyword", userSession.username())
        );
        assertSuccess(userListRoot);
        assertThat(userListRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode userDetailRoot = performJson(
                get("/api/admin/users/{id}", userSession.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(userDetailRoot);
        assertThat(userDetailRoot.path("data").path("username").asText()).isEqualTo(userSession.username());

        JsonNode statusRoot = performJson(
                put("/api/admin/users/{id}/status", userSession.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("status", 1)))
        );
        assertSuccess(statusRoot);

        JsonNode rolesUpdateRoot = performJson(
                put("/api/admin/users/{id}/roles", userSession.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("roleCodes", List.of("ROLE_USER"))))
        );
        assertSuccess(rolesUpdateRoot);

        JsonNode orderListRoot = performJson(
                get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .param("keyword", userSession.username())
        );
        assertSuccess(orderListRoot);
        assertThat(orderListRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode orderDetailRoot = performJson(
                get("/api/admin/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(orderDetailRoot);
        assertThat(orderDetailRoot.path("data").path("orderNo").asText()).isNotBlank();

        markOrderStatus(adminSession.token(), orderId, OrderStatusConstant.PAID_PENDING_TRAVEL);
        markOrderStatus(adminSession.token(), orderId, OrderStatusConstant.COMPLETED);

        JsonNode reviewableRoot = performJson(
                get("/api/orders/reviewable")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
        );
        assertSuccess(reviewableRoot);
        assertThat(reviewableRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode reviewRoot = performJson(
                post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "orderId", orderId,
                                "rating", 5,
                                "content", "集成测试评价内容"
                        )))
        );
        assertSuccess(reviewRoot);
        Long reviewId = reviewRoot.path("data").path("id").asLong();

        JsonNode currentReviewRoot = performJson(
                get("/api/orders/{id}/review", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
        );
        assertSuccess(currentReviewRoot);
        assertThat(currentReviewRoot.path("data").path("id").asLong()).isEqualTo(reviewId);

        JsonNode createShareRoot = performJson(
                post("/api/shares")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "Integration Share",
                                "summary", "share summary",
                                "content", "share content",
                                "imageUrls", List.of("/uploads/share-1.png", "/uploads/share-2.png")
                        )))
        );
        assertSuccess(createShareRoot);
        Long shareId = createShareRoot.path("data").path("id").asLong();

        JsonNode mySharesRoot = performJson(
                get("/api/shares/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userSession.token()))
        );
        assertSuccess(mySharesRoot);
        assertThat(mySharesRoot.path("data").path("records").size()).isEqualTo(1);

        JsonNode publicSharesRoot = performJson(
                get("/api/public/shares")
        );
        assertSuccess(publicSharesRoot);
        assertThat(publicSharesRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode publicShareDetailRoot = performJson(
                get("/api/public/shares/{id}", shareId)
        );
        assertSuccess(publicShareDetailRoot);
        assertThat(publicShareDetailRoot.path("data").path("imageUrls").size()).isEqualTo(2);

        JsonNode adminSharesRoot = performJson(
                get("/api/admin/shares")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .param("keyword", "Integration Share")
        );
        assertSuccess(adminSharesRoot);
        assertThat(adminSharesRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode adminReviewsRoot = performJson(
                get("/api/admin/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
                        .param("keyword", "集成测试评价内容")
        );
        assertSuccess(adminReviewsRoot);
        assertThat(adminReviewsRoot.path("data").path("records").size()).isGreaterThanOrEqualTo(1);

        JsonNode deleteReviewRoot = performJson(
                delete("/api/admin/reviews/{id}", reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(deleteReviewRoot);

        JsonNode deleteShareRoot = performJson(
                delete("/api/admin/shares/{id}", shareId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminSession.token()))
        );
        assertSuccess(deleteShareRoot);
    }
}
