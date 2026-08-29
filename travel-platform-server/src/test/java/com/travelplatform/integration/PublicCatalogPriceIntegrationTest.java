package com.travelplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class PublicCatalogPriceIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicCatalogQueriesShouldReturnActiveProductsAndDetails() throws Exception {
        JsonNode flights = performJson(get("/api/public/flights").param("pageSize", "10"));
        assertSuccess(flights);
        assertThat(flights.path("data").path("records").size()).isGreaterThan(0);
        Long flightId = flights.path("data").path("records").get(0).path("id").asLong();

        JsonNode flightDetail = performJson(get("/api/public/flights/{id}", flightId));
        assertSuccess(flightDetail);
        assertThat(flightDetail.path("data").path("id").asLong()).isEqualTo(flightId);
        assertThat(flightDetail.path("data").path("flightNo").asText()).isNotBlank();

        JsonNode trains = performJson(get("/api/public/trains").param("pageSize", "10"));
        assertSuccess(trains);
        assertThat(trains.path("data").path("records").size()).isGreaterThan(0);
        Long trainId = trains.path("data").path("records").get(0).path("id").asLong();
        JsonNode trainDetail = performJson(get("/api/public/trains/{id}", trainId));
        assertSuccess(trainDetail);
        assertThat(trainDetail.path("data").path("id").asLong()).isEqualTo(trainId);
        assertThat(trainDetail.path("data").path("trainNo").asText()).isNotBlank();

        JsonNode hotels = performJson(get("/api/public/hotels").param("pageSize", "10"));
        assertSuccess(hotels);
        assertThat(hotels.path("data").path("records").size()).isGreaterThan(0);
        Long hotelId = hotels.path("data").path("records").get(0).path("id").asLong();
        JsonNode hotelDetail = performJson(get("/api/public/hotels/{id}", hotelId));
        assertSuccess(hotelDetail);
        assertThat(hotelDetail.path("data").path("id").asLong()).isEqualTo(hotelId);
        assertThat(hotelDetail.path("data").path("hotelName").asText()).isNotBlank();

        JsonNode tours = performJson(get("/api/public/tours").param("pageSize", "10"));
        assertSuccess(tours);
        assertThat(tours.path("data").path("records").size()).isGreaterThan(0);
        Long tourId = tours.path("data").path("records").get(0).path("id").asLong();
        JsonNode tourDetail = performJson(get("/api/public/tours/{id}", tourId));
        assertSuccess(tourDetail);
        assertThat(tourDetail.path("data").path("id").asLong()).isEqualTo(tourId);
        assertThat(tourDetail.path("data").path("packageName").asText()).isNotBlank();
    }

    @Test
    void priceCompareShouldReturnCurrentAndLowestPriceForEachProductType() throws Exception {
        Long hotelId = findHotelId();
        JsonNode hotelCompare = performJson(get("/api/public/price-compare/hotels/{id}", hotelId));
        assertSuccess(hotelCompare);
        assertCompareResult(hotelCompare, "HOTEL", hotelId);

        Long flightId = findFutureFlightId();
        JsonNode flightCompare = performJson(get("/api/public/price-compare/flights/{id}", flightId));
        assertSuccess(flightCompare);
        assertCompareResult(flightCompare, "FLIGHT", flightId);

        Long tourId = findTourId();
        JsonNode tourCompare = performJson(get("/api/public/price-compare/tours/{id}", tourId));
        assertSuccess(tourCompare);
        assertCompareResult(tourCompare, "TOUR", tourId);
    }

    private void assertCompareResult(JsonNode root, String productType, Long productId) {
        JsonNode data = root.path("data");
        assertThat(data.path("productType").asText()).isEqualTo(productType);
        assertThat(data.path("productId").asLong()).isEqualTo(productId);
        BigDecimal currentPrice = new BigDecimal(data.path("currentPrice").asText());
        BigDecimal lowestPrice = new BigDecimal(data.path("lowestPrice").asText());
        assertThat(data.path("compareItems").size()).isGreaterThan(0);
        assertThat(currentPrice).isGreaterThanOrEqualTo(lowestPrice);
        assertThat(data.path("compareItems").findValuesAsText("productId")).contains(String.valueOf(productId));
        assertThat(data.path("lowPriceLabel").asText()).isNotBlank();
    }
}
