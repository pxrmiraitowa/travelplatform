package com.travelplatform.service.train.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.train.TrainQueryRequest;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.train.TrainDetailVO;
import com.travelplatform.vo.train.TrainListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainServiceImplTest {
    @Mock TrainTicketMapper mapper;
    @InjectMocks TrainServiceImpl service;

    @Test
    void searchTrainsShouldFilterByAvailableMinPrice() {
        when(mapper.selectList(any())).thenReturn(List.of(
                train(1L, new BigDecimal("260"), new BigDecimal("180"), 5),
                train(2L, new BigDecimal("300"), new BigDecimal("280"), 5)
        ));
        TrainQueryRequest request = new TrainQueryRequest();
        request.setTravelDate(LocalDate.of(2026, 1, 1));
        request.setMaxPrice(new BigDecimal("250"));

        PageResult<TrainListItemVO> result = service.searchTrains(request);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getTrainDetailShouldExposeSeatOptions() {
        when(mapper.selectById(1L)).thenReturn(train(1L, new BigDecimal("260"), new BigDecimal("180"), 5));

        TrainDetailVO result = service.getTrainDetail(1L);

        assertThat(result.getSeatOptions()).hasSize(3);
        assertThat(result.getMinPrice()).isEqualByComparingTo("180");
    }

    @Test
    void getTrainDetailShouldRejectMissingTicket() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getTrainDetail(1L)).isInstanceOf(BusinessException.class);
    }

    private TrainTicket train(Long id, BigDecimal businessPrice, BigDecimal secondPrice, Integer stock) {
        TrainTicket ticket = new TrainTicket();
        ticket.setId(id);
        ticket.setTrainNo("G100");
        ticket.setTrainType("G");
        ticket.setDepartureCity("Shanghai");
        ticket.setArrivalCity("Hangzhou");
        ticket.setDepartureStation("Shanghai Hongqiao");
        ticket.setArrivalStation("Hangzhou East");
        ticket.setDepartureTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        ticket.setArrivalTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        ticket.setDurationMinutes(60);
        ticket.setBusinessPrice(businessPrice);
        ticket.setBusinessStock(stock);
        ticket.setFirstClassPrice(new BigDecimal("290"));
        ticket.setFirstClassStock(stock);
        ticket.setSecondClassPrice(secondPrice);
        ticket.setSecondClassStock(stock);
        ticket.setStatus(1);
        return ticket;
    }
}
