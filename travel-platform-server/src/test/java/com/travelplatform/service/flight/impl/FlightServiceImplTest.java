package com.travelplatform.service.flight.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.flight.FlightQueryRequest;
import com.travelplatform.entity.Flight;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.flight.FlightDetailVO;
import com.travelplatform.vo.flight.FlightListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {
    @Mock FlightMapper mapper;
    @InjectMocks FlightServiceImpl service;

    @Test
    void searchFlightsShouldFilterByTimeRangeAndPaginate() {
        when(mapper.selectList(any())).thenReturn(List.of(
                flight(1L, LocalDateTime.of(2026,1,1,8,0), new BigDecimal("300")),
                flight(2L, LocalDateTime.of(2026,1,1,12,0), new BigDecimal("350"))
        ));
        FlightQueryRequest request = new FlightQueryRequest();
        request.setDepartureDate(LocalDate.of(2026, 1, 1));
        request.setDepartureStartTime(LocalTime.of(9, 0));

        PageResult<FlightListItemVO> result = service.searchFlights(request);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getFlightDetailShouldReturnMappedFields() {
        when(mapper.selectById(1L)).thenReturn(flight(1L, LocalDateTime.now(), new BigDecimal("300")));

        FlightDetailVO result = service.getFlightDetail(1L);

        assertThat(result.getFlightNo()).isEqualTo("MU1001");
        assertThat(result.getBaggagePolicy()).isEqualTo("20kg");
    }

    @Test
    void getFlightDetailShouldRejectMissingFlight() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getFlightDetail(1L)).isInstanceOf(BusinessException.class);
    }

    private Flight flight(Long id, LocalDateTime departure, BigDecimal price) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNo("MU1001");
        flight.setAirlineName("China Eastern");
        flight.setDepartureCity("Shanghai");
        flight.setArrivalCity("Beijing");
        flight.setDepartureAirport("PVG");
        flight.setArrivalAirport("PEK");
        flight.setDepartureTime(departure);
        flight.setArrivalTime(departure.plusHours(2));
        flight.setPrice(price);
        flight.setStock(5);
        flight.setCabinClass("Economy");
        flight.setBaggagePolicy("20kg");
        flight.setRefundPolicy("Flexible");
        flight.setStatus(1);
        return flight;
    }
}
