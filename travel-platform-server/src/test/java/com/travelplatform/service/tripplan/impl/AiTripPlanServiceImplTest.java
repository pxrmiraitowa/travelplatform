package com.travelplatform.service.tripplan.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.config.AiPlannerProperties;
import com.travelplatform.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveDayRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.entity.Attraction;
import com.travelplatform.entity.TripPlan;
import com.travelplatform.entity.TripPlanItem;
import com.travelplatform.mapper.AttractionMapper;
import com.travelplatform.mapper.TripPlanItemMapper;
import com.travelplatform.mapper.TripPlanMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTripPlanServiceImplTest {

    @Mock AttractionMapper attractionMapper;
    @Mock TripPlanMapper tripPlanMapper;
    @Mock TripPlanItemMapper tripPlanItemMapper;
    @InjectMocks AiTripPlanServiceImpl service;

    @Test
    void buildPreviewShouldUseDeterministicLocalFallbackWithoutCallingNetwork() {
        AiPlannerProperties properties = new AiPlannerProperties();
        properties.setEnabled(false);
        AiTripPlanServiceImpl localService = new AiTripPlanServiceImpl(
                attractionMapper, tripPlanMapper, tripPlanItemMapper, properties, new ObjectMapper());
        when(attractionMapper.selectList(any())).thenReturn(List.of(
                attraction(1L, "杭州", "西湖", "自然风光", 10),
                attraction(2L, "杭州", "灵隐寺", "人文历史", 8)
        ));

        AiTripPlanPreviewRequest request = new AiTripPlanPreviewRequest();
        request.setDestination(" 杭州市 ");
        request.setTotalDays(2);
        request.setPreferences(List.of(" 自然风光 ", "自然风光", " "));
        request.setStartDate(LocalDate.of(2026, 9, 1));

        AiTripPlanPreviewVO result = localService.buildPreview(request);

        assertThat(result.getDestination()).isEqualTo("杭州");
        assertThat(result.getGenerationMode()).isEqualTo("LOCAL_FALLBACK");
        assertThat(result.getDays()).hasSize(2);
        assertThat(result.getPreferences()).containsExactly("自然风光");
    }

    @Test
    void buildPreviewShouldRejectDestinationWithoutActiveAttractions() {
        AiPlannerProperties properties = new AiPlannerProperties();
        properties.setEnabled(false);
        AiTripPlanServiceImpl localService = new AiTripPlanServiceImpl(
                attractionMapper, tripPlanMapper, tripPlanItemMapper, properties, new ObjectMapper());
        when(attractionMapper.selectList(any())).thenReturn(List.of());
        AiTripPlanPreviewRequest request = new AiTripPlanPreviewRequest();
        request.setDestination("不存在");
        request.setTotalDays(1);

        BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> localService.buildPreview(request), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
        verify(tripPlanMapper, never()).insert(any(TripPlan.class));
    }

    @Test
    void savePlanShouldRejectDuplicateDayAndInvalidAttractionBeforeInsert() {
        AiTripPlanSaveRequest request = saveRequest(2, List.of(day(1, 1L), day(1, 2L)));
        when(attractionMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(
                attraction(1L, "杭州", "西湖", "自然", 1), attraction(2L, "杭州", "灵隐寺", "人文", 1)));

        BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.savePlan(request), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        verify(tripPlanMapper, never()).insert(any(TripPlan.class));
    }

    @Test
    void savePlanShouldRejectAttractionFromAnotherCity() {
        AiTripPlanSaveRequest request = saveRequest(1, List.of(day(1, 1L)));
        when(attractionMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(
                attraction(1L, "上海", "外滩", "城市地标", 1)));

        BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.savePlan(request), BusinessException.class);

        assertThat(error.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        verify(tripPlanMapper, never()).insert(any(TripPlan.class));
    }

    @Test
    void savePlanShouldPersistAiPlanForCurrentUserAndSortedDays() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            AiTripPlanSaveRequest request = saveRequest(2, List.of(day(2, 2L), day(1, 1L)));
            when(attractionMapper.selectBatchIds(List.of(2L, 1L))).thenReturn(List.of(
                    attraction(1L, "杭州", "西湖", "自然", 1), attraction(2L, "杭州", "灵隐寺", "人文", 1)));
            ArgumentCaptor<TripPlan> planCaptor = ArgumentCaptor.forClass(TripPlan.class);
            when(tripPlanMapper.insert(planCaptor.capture())).thenAnswer(invocation -> {
                planCaptor.getValue().setId(31L);
                return 1;
            });

            TripPlanDetailVO result = service.savePlan(request);

            assertThat(planCaptor.getValue().getUserId()).isEqualTo(7L);
            assertThat(planCaptor.getValue().getSourceType()).isEqualTo("AI");
            assertThat(result.getId()).isEqualTo(31L);
            verify(tripPlanItemMapper, org.mockito.Mockito.times(2)).insert(any(TripPlanItem.class));
        }
    }

    private AiTripPlanSaveRequest saveRequest(int totalDays, List<AiTripPlanSaveDayRequest> days) {
        AiTripPlanSaveRequest request = new AiTripPlanSaveRequest();
        request.setPlanName("杭州 AI 行程");
        request.setDestination("杭州市");
        request.setTotalDays(totalDays);
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setDays(days);
        return request;
    }

    private AiTripPlanSaveDayRequest day(int dayNo, Long attractionId) {
        AiTripPlanSaveDayRequest day = new AiTripPlanSaveDayRequest();
        day.setDayNo(dayNo);
        day.setAttractionIds(List.of(attractionId));
        day.setReason("适合当天游览");
        return day;
    }

    private Attraction attraction(Long id, String city, String name, String type, int priority) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setCity(city);
        attraction.setDistrict("西湖");
        attraction.setAttractionName(name);
        attraction.setAttractionType(type);
        attraction.setPriority(priority);
        attraction.setStatus(1);
        attraction.setTags(type);
        return attraction;
    }
}
