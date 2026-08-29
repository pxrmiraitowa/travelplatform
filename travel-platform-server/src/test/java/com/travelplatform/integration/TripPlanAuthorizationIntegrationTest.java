package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
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

class TripPlanAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void manualTripPlanShouldSupportCrudAndPersistOwnedItems() throws Exception {
        AuthSession session = registerUserAndLogin();
        JsonNode created = performJson(post("/api/trip-plans")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "planName", "Integration Shanghai Plan",
                        "totalDays", 2,
                        "startDate", futureDate(2),
                        "remark", "manual flow"
                ))));
        assertSuccess(created);
        Long planId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("sourceType").asText()).isEqualTo("MANUAL");
        assertThat(created.path("data").path("items").size()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan where id = ? and user_id = ?",
                Integer.class, planId, session.userId())).isEqualTo(1);

        Long dayOneItemId = createItem(session.token(), planId, 1, "上海", "地铁");
        Long dayTwoItemId = createItem(session.token(), planId, 2, "杭州", "高铁");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan_item where plan_id = ?",
                Integer.class, planId)).isEqualTo(2);

        JsonNode detail = performJson(get("/api/trip-plans/{id}", planId)
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token())));
        assertSuccess(detail);
        assertThat(detail.path("data").path("items").size()).isEqualTo(2);
        assertThat(detail.path("data").path("items").get(0).path("dayNo").asInt()).isEqualTo(1);
        assertThat(detail.path("data").path("items").get(1).path("dayNo").asInt()).isEqualTo(2);

        JsonNode duplicateDay = performJson(put("/api/trip-plans/{planId}/items/{itemId}", planId, dayOneItemId)
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "dayNo", 2,
                        "destination", "苏州",
                        "transportType", "高铁"
                ))));
        assertFailure(duplicateDay, 400);
        assertThat(jdbcTemplate.queryForObject(
                "select day_no from trip_plan_item where id = ? and plan_id = ?",
                Integer.class, dayOneItemId, planId)).isEqualTo(1);

        JsonNode shrinkPlan = performJson(put("/api/trip-plans/{id}", planId)
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "planName", "Shortened Plan",
                        "totalDays", 1,
                        "startDate", futureDate(2)
                ))));
        assertFailure(shrinkPlan, 400);

        JsonNode plans = performJson(get("/api/trip-plans")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token())));
        assertSuccess(plans);
        assertThat(plans.path("data").size()).isEqualTo(1);
        assertThat(plans.path("data").get(0).path("itemCount").asInt()).isEqualTo(2);

        JsonNode deletedItem = performJson(delete("/api/trip-plans/{planId}/items/{itemId}", planId, dayTwoItemId)
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token())));
        assertSuccess(deletedItem);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan_item where plan_id = ?",
                Integer.class, planId)).isEqualTo(1);

        JsonNode deletedPlan = performJson(delete("/api/trip-plans/{id}", planId)
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token())));
        assertSuccess(deletedPlan);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan where id = ? and user_id = ?",
                Integer.class, planId, session.userId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan_item where plan_id = ?",
                Integer.class, planId)).isZero();
    }

    @Test
    void aiTripPlanShouldUseLocalFallbackAndSaveAttractionItems() throws Exception {
        AuthSession session = registerUserAndLogin();
        Long attractionId = jdbcTemplate.queryForObject(
                "select id from attraction where city = '上海' and status = 1 order by id limit 1", Long.class);
        String attractionDistrict = jdbcTemplate.queryForObject(
                "select district from attraction where id = ?", String.class, attractionId);

        JsonNode preview = performJson(post("/api/trip-plans/ai-preview")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "destination", "上海",
                        "totalDays", 1,
                        "startDate", futureDate(3),
                        "preferences", List.of("城市地标")
                ))));
        assertSuccess(preview);
        assertThat(preview.path("data").path("sourceType").asText()).isEqualTo("AI");
        assertThat(preview.path("data").path("generationMode").asText()).isEqualTo("LOCAL_FALLBACK");
        assertThat(preview.path("data").path("days").size()).isEqualTo(1);
        assertThat(preview.path("data").path("days").get(0).path("attractions").size()).isGreaterThan(0);

        JsonNode saved = performJson(post("/api/trip-plans/ai-save")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "planName", "Saved AI Plan",
                        "destination", "上海",
                        "totalDays", 1,
                        "startDate", futureDate(3),
                        "preferences", List.of("城市地标"),
                        "days", List.of(Map.of(
                                "dayNo", 1,
                                "attractionIds", List.of(attractionId),
                                "reason", "integration fallback"
                        ))
                ))));
        assertSuccess(saved);
        Long planId = saved.path("data").path("id").asLong();
        assertThat(saved.path("data").path("sourceType").asText()).isEqualTo("AI");
        assertThat(saved.path("data").path("items").size()).isEqualTo(1);
        assertThat(saved.path("data").path("items").get(0).path("destination").asText())
                .isEqualTo(attractionDistrict + "片区");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan where id = ? and user_id = ? and source_type = 'AI'",
                Integer.class, planId, session.userId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trip_plan_item where plan_id = ? and day_no = 1",
                Integer.class, planId)).isEqualTo(1);
    }

    @Test
    void ordinaryUserShouldBeDeniedAdminEndpoint() throws Exception {
        AuthSession session = registerUserAndLogin();
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    private Long createItem(String token, Long planId, int dayNo, String destination, String transportType) throws Exception {
        JsonNode root = performJson(post("/api/trip-plans/{id}/items", planId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                        "dayNo", dayNo,
                        "destination", destination,
                        "transportType", transportType,
                        "remark", "item"
                ))));
        assertSuccess(root);
        return root.path("data").path("id").asLong();
    }
}
