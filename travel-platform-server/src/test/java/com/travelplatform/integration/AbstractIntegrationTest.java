package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    private static final String TEST_USER_PREFIX = "iu";
    private static final String TEST_ADMIN_PREFIX = "ia_";
    private static final DateTimeFormatter UNIQUE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUpTestData() {
        List<Long> testUserIds = jdbcTemplate.queryForList(
                "select id from `user` where username like ? or username like ?",
                Long.class,
                TEST_USER_PREFIX + "%",
                TEST_ADMIN_PREFIX + "%"
        );
        if (testUserIds.isEmpty()) {
            return;
        }

        String userIdClause = joinIds(testUserIds);
        restoreFlightStock(userIdClause);
        restoreTrainStock(userIdClause);
        restoreHotelStock(userIdClause);
        restoreTourStock(userIdClause);

        jdbcTemplate.execute("delete from share_image where post_id in (select id from share_post where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from share_post where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from review where user_id in (" + userIdClause + ") or order_id in (select id from orders where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from order_flight where order_id in (select id from orders where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from order_train where order_id in (select id from orders where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from order_hotel where order_id in (select id from orders where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from order_tour where order_id in (select id from orders where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from orders where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from price_alert where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from trip_plan_item where plan_id in (select id from trip_plan where user_id in (" + userIdClause + "))");
        jdbcTemplate.execute("delete from trip_plan where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from user_contact where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from user_role where user_id in (" + userIdClause + ")");
        jdbcTemplate.execute("delete from `user` where id in (" + userIdClause + ")");
    }

    protected AuthSession registerUserAndLogin() throws Exception {
        String suffix = uniqueSuffix();
        String username = TEST_USER_PREFIX + suffix.substring(0, 14);
        String phone = "18" + suffix.substring(Math.max(0, suffix.length() - 9));
        Map<String, Object> request = Map.of(
                "username", username,
                "nickname", "Integration User",
                "phone", phone,
                "password", "123456",
                "confirmPassword", "123456"
        );

        JsonNode root = performJson(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        );
        assertSuccess(root);
        return new AuthSession(
                root.path("data").path("token").asText(),
                root.path("data").path("userInfo").path("id").asLong(),
                username,
                phone
        );
    }

    protected AuthSession ensureAdminAndLogin() throws Exception {
        String username = TEST_ADMIN_PREFIX + "main";
        Long existingId = jdbcTemplate.query(
                "select id from `user` where username = ? limit 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                username
        );
        if (existingId == null) {
            jdbcTemplate.update(
                    "insert into `user` (username, password, nickname, phone, status) values (?, ?, ?, ?, ?)",
                    username,
                    passwordEncoder.encode("123456"),
                    "Integration Admin",
                    "19900000001",
                    1
            );
            Long userId = jdbcTemplate.queryForObject("select id from `user` where username = ?", Long.class, username);
            List<Long> roleIds = jdbcTemplate.queryForList(
                    "select id from role where role_code in ('ROLE_USER', 'ROLE_ADMIN') order by id",
                    Long.class
            );
            for (Long roleId : roleIds) {
                jdbcTemplate.update("insert into user_role (user_id, role_id) values (?, ?)", userId, roleId);
            }
        }

        JsonNode root = performJson(
                post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", username, "password", "123456")))
        );
        assertSuccess(root);
        return new AuthSession(
                root.path("data").path("token").asText(),
                root.path("data").path("userInfo").path("id").asLong(),
                username,
                "19900000001"
        );
    }

    protected Long createContact(String token, String name) throws Exception {
        JsonNode root = performJson(
                post("/api/user-contacts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "name", name,
                                "phone", "13912345678",
                                "idCard", "310101199001011234",
                                "contactType", 1,
                                "isDefault", 1,
                                "remark", "integration"
                        )))
        );
        assertSuccess(root);
        return root.path("data").path("id").asLong();
    }

    protected Long findFutureFlightId() {
        return jdbcTemplate.queryForObject(
                "select id from flight where status = 1 and departure_time > ? order by departure_time limit 1",
                Long.class,
                LocalDateTime.now()
        );
    }

    protected Long findFutureTrainId() {
        return jdbcTemplate.queryForObject(
                "select id from train_ticket where status = 1 and departure_time > ? order by departure_time limit 1",
                Long.class,
                LocalDateTime.now()
        );
    }

    protected Long findHotelId() {
        return jdbcTemplate.queryForObject(
                "select id from hotel where status = 1 order by id limit 1",
                Long.class
        );
    }

    protected Long findHotelRoomId(Long hotelId) {
        return jdbcTemplate.queryForObject(
                "select id from hotel_room where hotel_id = ? and status = 1 and stock > 0 order by id limit 1",
                Long.class,
                hotelId
        );
    }

    protected Long findTourId() {
        return jdbcTemplate.queryForObject(
                "select id from tour_package where status = 1 and stock > 0 order by id limit 1",
                Long.class
        );
    }

    protected Long findCouponId(String productType) {
        return jdbcTemplate.query(
                "select id from coupon where product_type = ? and status = 1 and start_time <= ? and end_time >= ? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                productType,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    protected String firstTravelDateForTour(Long tourId) {
        String rawDates = jdbcTemplate.queryForObject(
                "select travel_dates from tour_package where id = ?",
                String.class,
                tourId
        );
        return Arrays.stream(rawDates.split(","))
                .map(String::trim)
                .filter(value -> !LocalDate.parse(value).isBefore(LocalDate.now()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid future travel date found for tour " + tourId));
    }

    protected void markOrderStatus(String adminToken, Long orderId, int status) throws Exception {
        JsonNode root = performJson(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("orderStatus", status)))
        );
        assertSuccess(root);
    }

    protected JsonNode performJson(RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected void assertSuccess(JsonNode root) {
        assertThat(root.path("code").asInt()).isEqualTo(200);
    }

    protected void assertFailure(JsonNode root, int expectedCode) {
        assertThat(root.path("code").asInt()).isEqualTo(expectedCode);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String uniqueSuffix() {
        return UNIQUE_FORMATTER.format(LocalDateTime.now());
    }

    protected String futureDate(int plusDays) {
        return LocalDate.now().plusDays(plusDays).toString();
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("0");
    }

    private void restoreFlightStock(String userIdClause) {
        jdbcTemplate.query(
                "select f.flight_id from order_flight f "
                        + "join orders o on o.id = f.order_id "
                        + "where o.user_id in (" + userIdClause + ") and o.order_status <> 40",
                rs -> {
                    while (rs.next()) {
                        jdbcTemplate.update("update flight set stock = coalesce(stock, 0) + 1 where id = ?", rs.getLong(1));
                    }
                    return null;
                }
        );
    }

    private void restoreTrainStock(String userIdClause) {
        jdbcTemplate.query(
                "select t.train_ticket_id, t.seat_type from order_train t "
                        + "join orders o on o.id = t.order_id "
                        + "where o.user_id in (" + userIdClause + ") and o.order_status <> 40",
                rs -> {
                    while (rs.next()) {
                        Long trainTicketId = rs.getLong(1);
                        String seatType = rs.getString(2);
                        String column = switch (seatType) {
                            case "商务座" -> "business_stock";
                            case "一等座" -> "first_class_stock";
                            case "二等座" -> "second_class_stock";
                            default -> null;
                        };
                        if (column != null) {
                            jdbcTemplate.update("update train_ticket set " + column + " = coalesce(" + column + ", 0) + 1 where id = ?", trainTicketId);
                        }
                    }
                    return null;
                }
        );
    }

    private void restoreHotelStock(String userIdClause) {
        jdbcTemplate.query(
                "select h.hotel_room_id from order_hotel h "
                        + "join orders o on o.id = h.order_id "
                        + "where o.user_id in (" + userIdClause + ") and o.order_status <> 40",
                rs -> {
                    while (rs.next()) {
                        jdbcTemplate.update("update hotel_room set stock = coalesce(stock, 0) + 1 where id = ?", rs.getLong(1));
                    }
                    return null;
                }
        );
    }

    private void restoreTourStock(String userIdClause) {
        jdbcTemplate.query(
                "select t.tour_package_id from order_tour t "
                        + "join orders o on o.id = t.order_id "
                        + "where o.user_id in (" + userIdClause + ") and o.order_status <> 40",
                rs -> {
                    while (rs.next()) {
                        jdbcTemplate.update("update tour_package set stock = coalesce(stock, 0) + 1 where id = ?", rs.getLong(1));
                    }
                    return null;
                }
        );
    }

    protected record AuthSession(String token, Long userId, String username, String phone) {
    }
}
