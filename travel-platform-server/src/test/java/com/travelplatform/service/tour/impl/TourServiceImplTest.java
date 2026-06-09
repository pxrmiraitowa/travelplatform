package com.travelplatform.service.tour.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.tour.TourDetailVO;
import com.travelplatform.vo.tour.TourListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceImplTest {
    @Mock TourPackageMapper mapper;
    @InjectMocks TourServiceImpl service;

    @Test
    void listToursShouldPaginateMappedRecords() {
        when(mapper.selectList(any())).thenReturn(List.of(tour(1L), tour(2L)));

        PageResult<TourListItemVO> result = service.listTours("Sanya", 1, 1);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void getTourDetailShouldParseTravelDatesAndImages() {
        when(mapper.selectById(1L)).thenReturn(tour(1L));

        TourDetailVO result = service.getTourDetail(1L);

        assertThat(result.getTravelDateOptions()).containsExactly("2026-01-01", "2026-01-02");
        assertThat(result.getDetailImages()).isNotEmpty();
    }

    @Test
    void getTourDetailShouldRejectMissingTour() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getTourDetail(1L)).isInstanceOf(BusinessException.class);
    }

    private TourPackage tour(Long id) {
        TourPackage tour = new TourPackage();
        tour.setId(id);
        tour.setPackageName("Sanya Tour");
        tour.setDestination("Sanya");
        tour.setDepartureCity("Shanghai");
        tour.setDays(3);
        tour.setPrice(new BigDecimal("1999"));
        tour.setStock(10);
        tour.setTravelDates("2026-01-01, 2026-01-02");
        tour.setDescription("Desc");
        tour.setCoverImage("/cover.jpg");
        tour.setDetailImages("/a.jpg,/b.jpg");
        tour.setStatus(1);
        return tour;
    }
}
