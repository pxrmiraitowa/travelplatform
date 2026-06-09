package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.constant.OrderStatusConstant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void flightOrderShouldSupportCreateListDetailAndCancel() throws Exception {
        AuthSession session = registerUserAndLogin();
        Long contactId = createContact(session.token(), "Flight Passenger");

        JsonNode createRoot = performJson(
                post("/api/orders/flights")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "flightId", findFutureFlightId(),
                                "contactId", contactId,
                                "couponId", findCouponId("FLIGHT"),
                                "remark", "flight integration"
                        )))
        );
        assertSuccess(createRoot);
        Long orderId = createRoot.path("data").path("id").asLong();
        assertThat(createRoot.path("data").path("bizType").asText()).isEqualTo("FLIGHT");

        JsonNode listRoot = performJson(
                get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(listRoot);
        assertThat(listRoot.path("data").path("records").size()).isEqualTo(1);

        JsonNode detailRoot = performJson(
                get("/api/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(detailRoot);
        assertThat(detailRoot.path("data").path("flightInfo").path("flightNo").asText()).isNotBlank();

        JsonNode cancelRoot = performJson(
                post("/api/orders/{id}/cancel", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(cancelRoot);

        JsonNode cancelledDetailRoot = performJson(
                get("/api/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
        );
        assertSuccess(cancelledDetailRoot);
        assertThat(cancelledDetailRoot.path("data").path("orderStatus").asInt()).isEqualTo(OrderStatusConstant.CANCELLED);
    }

    @Test
    void trainHotelAndTourOrdersShouldSupportCreation() throws Exception {
        AuthSession session = registerUserAndLogin();
        Long contactId = createContact(session.token(), "Multi Product Passenger");

        JsonNode trainRoot = performJson(
                post("/api/orders/trains")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "trainTicketId", findFutureTrainId(),
                                "contactId", contactId,
                                "seatType", "一等座",
                                "remark", "train integration"
                        )))
        );
        assertSuccess(trainRoot);
        assertThat(trainRoot.path("data").path("trainInfo").path("trainNo").asText()).isNotBlank();

        Long hotelId = findHotelId();
        JsonNode hotelRoot = performJson(
                post("/api/orders/hotels")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "hotelId", hotelId,
                                "hotelRoomId", findHotelRoomId(hotelId),
                                "checkInDate", futureDate(3),
                                "checkOutDate", futureDate(4),
                                "contactId", contactId,
                                "couponId", findCouponId("HOTEL"),
                                "remark", "hotel integration"
                        )))
        );
        assertSuccess(hotelRoot);
        assertThat(hotelRoot.path("data").path("hotelInfo").path("hotelName").asText()).isNotBlank();

        Long tourId = findTourId();
        JsonNode tourRoot = performJson(
                post("/api/orders/tours")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "tourPackageId", tourId,
                                "travelDate", firstTravelDateForTour(tourId),
                                "contactId", contactId,
                                "couponId", findCouponId("TOUR"),
                                "remark", "tour integration"
                        )))
        );
        assertSuccess(tourRoot);
        assertThat(tourRoot.path("data").path("tourInfo").path("packageName").asText()).isNotBlank();
    }
}
