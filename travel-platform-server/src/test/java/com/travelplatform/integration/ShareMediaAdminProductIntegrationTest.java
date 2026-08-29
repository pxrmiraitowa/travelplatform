package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.constant.OrderStatusConstant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShareMediaAdminProductIntegrationTest extends AbstractIntegrationTest {

    @Test
    void userAndAdminImageUploadsShouldPersistFilesAndRespectAuthorization() throws Exception {
        AuthSession user = registerUserAndLogin();
        AuthSession admin = ensureAdminAndLogin();
        byte[] image = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47};

        JsonNode shareRoot = performJson(
                multipart("/api/shares/upload")
                        .file(new MockMultipartFile("file", "share.png", "image/png", image))
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
        );
        assertSuccess(shareRoot);
        assertUploadedFileExists(shareRoot.path("data").path("url").asText(), "share");

        JsonNode productRoot = performJson(
                multipart("/api/admin/media/upload")
                        .file(new MockMultipartFile("file", "product.png", "image/png", image))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
        );
        assertSuccess(productRoot);
        assertUploadedFileExists(productRoot.path("data").path("url").asText(), "product");

        mockMvc.perform(
                        multipart("/api/admin/media/upload")
                                .file(new MockMultipartFile("file", "product.png", "image/png", image))
                                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldSupportCrudForAllProductTypes() throws Exception {
        AuthSession admin = ensureAdminAndLogin();
        String suffix = uniqueSuffix();
        String flightNo = "IT" + suffix.substring(0, 10);
        String trainNo = "G" + suffix.substring(0, 11);

        assertListSuccess(admin.token(), "/api/admin/flights");
        assertListSuccess(admin.token(), "/api/admin/trains");
        assertListSuccess(admin.token(), "/api/admin/hotels");
        assertListSuccess(admin.token(), "/api/admin/tours");

        JsonNode flight = performJson(post("/api/admin/flights")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(flightRequest(flightNo, "集成航司", "测试出发", "测试到达", 2))));
        assertSuccess(flight);
        Long flightId = flight.path("data").path("id").asLong();
        assertThat(flight.path("data").path("flightNo").asText()).isEqualTo(flightNo);
        JsonNode updatedFlight = performJson(put("/api/admin/flights/{id}", flightId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(flightRequest(flightNo, "更新航司", "测试出发", "测试到达", 3))));
        assertSuccess(updatedFlight);
        assertThat(updatedFlight.path("data").path("airlineName").asText()).isEqualTo("更新航司");
        assertSuccess(performJson(delete("/api/admin/flights/{id}", flightId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));

        JsonNode train = performJson(post("/api/admin/trains")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(trainRequest(trainNo, 2))));
        assertSuccess(train);
        Long trainId = train.path("data").path("id").asLong();
        assertSuccess(performJson(put("/api/admin/trains/{id}", trainId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(trainRequest(trainNo, 3)))));
        assertSuccess(performJson(delete("/api/admin/trains/{id}", trainId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));

        String hotelName = "集成酒店-" + suffix;
        JsonNode hotel = performJson(post("/api/admin/hotels")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(hotelRequest(hotelName))));
        assertSuccess(hotel);
        Long hotelId = hotel.path("data").path("id").asLong();
        JsonNode room = performJson(post("/api/admin/hotel-rooms")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(roomRequest(hotelId, "集成大床房", "299.00"))));
        assertSuccess(room);
        Long roomId = room.path("data").path("id").asLong();
        JsonNode rooms = performJson(get("/api/admin/hotel-rooms")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .param("hotelId", hotelId.toString()));
        assertSuccess(rooms);
        assertThat(rooms.path("data").path("records").toString()).contains("集成大床房");
        assertSuccess(performJson(put("/api/admin/hotel-rooms/{id}", roomId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(roomRequest(hotelId, "更新大床房", "399.00")))));
        assertSuccess(performJson(delete("/api/admin/hotel-rooms/{id}", roomId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));
        assertSuccess(performJson(put("/api/admin/hotels/{id}", hotelId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(hotelRequest(hotelName + "-更新")))));
        assertSuccess(performJson(delete("/api/admin/hotels/{id}", hotelId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));

        String packageName = "集成旅游产品-" + suffix;
        JsonNode tour = performJson(post("/api/admin/tours")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(tourRequest(packageName, 2))));
        assertSuccess(tour);
        Long tourId = tour.path("data").path("id").asLong();
        JsonNode updatedTour = performJson(put("/api/admin/tours/{id}", tourId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(tourRequest(packageName + "-更新", 3))));
        assertSuccess(updatedTour);
        assertThat(updatedTour.path("data").path("days").asInt()).isEqualTo(3);
        assertSuccess(performJson(delete("/api/admin/tours/{id}", tourId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));
    }

    @Test
    void adminShouldCancelUserFlightOrderAndRestoreStock() throws Exception {
        AuthSession user = registerUserAndLogin();
        AuthSession admin = ensureAdminAndLogin();
        Long contactId = createContact(user.token(), "管理员取消订单乘客");
        Long flightId = findFutureFlightId();
        Integer stockBefore = jdbcTemplate.queryForObject("select stock from flight where id = ?", Integer.class, flightId);

        JsonNode order = performJson(post("/api/orders/flights")
                .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("flightId", flightId, "contactId", contactId, "remark", "admin cancel integration"))));
        assertSuccess(order);
        Long orderId = order.path("data").path("id").asLong();
        Integer stockAfterCreate = jdbcTemplate.queryForObject("select stock from flight where id = ?", Integer.class, flightId);
        assertThat(stockAfterCreate).isEqualTo(stockBefore - 1);

        assertSuccess(performJson(post("/api/admin/orders/{id}/cancel", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))));
        Integer stockAfterCancel = jdbcTemplate.queryForObject("select stock from flight where id = ?", Integer.class, flightId);
        assertThat(stockAfterCancel).isEqualTo(stockBefore);
        JsonNode detail = performJson(get("/api/admin/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin.token())));
        assertSuccess(detail);
        assertThat(detail.path("data").path("orderStatus").asInt()).isEqualTo(OrderStatusConstant.CANCELLED);
    }

    private void assertListSuccess(String token, String path) throws Exception {
        JsonNode root = performJson(get(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
        assertSuccess(root);
        assertThat(root.path("data").path("records").isArray()).isTrue();
    }

    private void assertUploadedFileExists(String url, String category) {
        assertThat(url).startsWith("/api/public/uploads/" + category + "/");
        String relative = url.substring("/api/public/uploads/".length());
        Path target = Paths.get("target", "test-uploads").resolve(relative).normalize().toAbsolutePath();
        assertThat(target.toString()).doesNotContain(".." + java.io.File.separator);
        assertThat(Files.exists(target)).isTrue();
    }

    private Map<String, Object> flightRequest(String flightNo, String airline, String departure, String arrival, int price) {
        LocalDateTime start = LocalDateTime.now().plusDays(30);
        return Map.ofEntries(Map.entry("flightNo", flightNo), Map.entry("airlineName", airline),
                Map.entry("departureCity", departure), Map.entry("arrivalCity", arrival),
                Map.entry("departureAirport", "测试机场A"), Map.entry("arrivalAirport", "测试机场B"),
                Map.entry("departureTime", start.toString()), Map.entry("arrivalTime", start.plusHours(2).toString()),
                Map.entry("price", BigDecimal.valueOf(price)), Map.entry("stock", 5),
                Map.entry("cabinClass", "经济舱"), Map.entry("baggagePolicy", "20kg"),
                Map.entry("refundPolicy", "可退"), Map.entry("status", 1));
    }

    private Map<String, Object> trainRequest(String trainNo, int price) {
        LocalDateTime start = LocalDateTime.now().plusDays(30);
        return Map.ofEntries(Map.entry("trainNo", trainNo), Map.entry("trainType", "高铁"),
                Map.entry("departureCity", "测试出发"), Map.entry("arrivalCity", "测试到达"),
                Map.entry("departureStation", "测试站A"), Map.entry("arrivalStation", "测试站B"),
                Map.entry("departureTime", start.toString()), Map.entry("arrivalTime", start.plusHours(3).toString()),
                Map.entry("durationMinutes", 180), Map.entry("businessPrice", BigDecimal.valueOf(price * 3L)),
                Map.entry("firstClassPrice", BigDecimal.valueOf(price * 2L)), Map.entry("secondClassPrice", BigDecimal.valueOf(price)),
                Map.entry("businessStock", 2), Map.entry("firstClassStock", 3),
                Map.entry("secondClassStock", 4), Map.entry("status", 1));
    }

    private Map<String, Object> hotelRequest(String hotelName) {
        return Map.ofEntries(Map.entry("hotelName", hotelName), Map.entry("city", "测试城市"),
                Map.entry("district", "测试片区"), Map.entry("address", "测试地址1号"),
                Map.entry("description", "管理员集成测试酒店"), Map.entry("starLevel", 4),
                Map.entry("coverImage", "/api/public/uploads/product/test.png"),
                Map.entry("detailImages", "/api/public/uploads/product/detail.png"),
                Map.entry("checkInTime", "14:00"), Map.entry("checkOutTime", "12:00"), Map.entry("status", 1));
    }

    private Map<String, Object> roomRequest(Long hotelId, String roomName, String price) {
        return Map.ofEntries(Map.entry("hotelId", hotelId), Map.entry("roomName", roomName),
                Map.entry("bedType", "大床"), Map.entry("breakfast", "含早"),
                Map.entry("roomArea", "35平方米"), Map.entry("guestCount", 2),
                Map.entry("price", new BigDecimal(price)), Map.entry("stock", 5),
                Map.entry("cancelRule", "入住前可退"), Map.entry("status", 1));
    }

    private Map<String, Object> tourRequest(String packageName, int days) {
        String date = LocalDate.now().plusDays(60).toString();
        return Map.ofEntries(Map.entry("packageName", packageName), Map.entry("destination", "测试景区"),
                Map.entry("departureCity", "测试城市"), Map.entry("days", days),
                Map.entry("price", BigDecimal.valueOf(999)), Map.entry("stock", 5),
                Map.entry("travelDates", date), Map.entry("description", "管理员集成测试旅游产品"),
                Map.entry("coverImage", "/api/public/uploads/product/tour.png"),
                Map.entry("detailImages", "/api/public/uploads/product/tour-detail.png"), Map.entry("status", 1));
    }
}
