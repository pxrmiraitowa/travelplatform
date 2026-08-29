package com.travelplatform.service.tripplan.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.tripplan.TripPlanCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemUpdateRequest;
import com.travelplatform.dto.tripplan.TripPlanUpdateRequest;
import com.travelplatform.entity.TripPlan;
import com.travelplatform.entity.TripPlanItem;
import com.travelplatform.mapper.TripPlanItemMapper;
import com.travelplatform.mapper.TripPlanMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.vo.tripplan.TripPlanItemVO;
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
class TripPlanServiceImplTest {

    @Mock TripPlanMapper tripPlanMapper;
    @Mock TripPlanItemMapper tripPlanItemMapper;
    @InjectMocks TripPlanServiceImpl service;

    @Test
    void createPlanShouldBindCurrentUserAndManualSource() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            TripPlanCreateRequest request = createRequest(3);
            ArgumentCaptor<TripPlan> captor = ArgumentCaptor.forClass(TripPlan.class);
            when(tripPlanMapper.insert(captor.capture())).thenAnswer(invocation -> {
                captor.getValue().setId(11L);
                return 1;
            });

            TripPlanDetailVO result = service.createPlan(request);

            assertThat(captor.getValue().getUserId()).isEqualTo(7L);
            assertThat(captor.getValue().getSourceType()).isEqualTo("MANUAL");
            assertThat(result.getId()).isEqualTo(11L);
            assertThat(result.getTotalDays()).isEqualTo(3);
        }
    }

    @Test
    void getPlanDetailShouldRejectAnotherUsersPlanWithNotFoundCode() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 8L, 3));

            BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> service.getCurrentUserPlanDetail(11L), BusinessException.class);

            assertThat(error.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
            verify(tripPlanItemMapper, never()).selectList(any());
        }
    }

    @Test
    void createPlanItemShouldRejectDayOutsidePlanBoundary() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 7L, 3));
            TripPlanItemCreateRequest request = itemCreateRequest(4);

            BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> service.createPlanItem(11L, request), BusinessException.class);

            assertThat(error.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            verify(tripPlanItemMapper, never()).insert(any(TripPlanItem.class));
        }
    }

    @Test
    void createPlanItemShouldRejectDuplicateDayAndPersistValidItem() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 7L, 3));
            TripPlanItem existing = item(21L, 11L, 2);
            when(tripPlanItemMapper.selectList(any())).thenReturn(List.of(existing));

            BusinessException duplicate = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> service.createPlanItem(11L, itemCreateRequest(2)), BusinessException.class);
            assertThat(duplicate.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            verify(tripPlanItemMapper, never()).insert(any(TripPlanItem.class));

            when(tripPlanItemMapper.selectList(any())).thenReturn(List.of());
            ArgumentCaptor<TripPlanItem> captor = ArgumentCaptor.forClass(TripPlanItem.class);
            when(tripPlanItemMapper.insert(captor.capture())).thenAnswer(invocation -> {
                captor.getValue().setId(22L);
                return 1;
            });
            TripPlanItemVO result = service.createPlanItem(11L, itemCreateRequest(1));

            assertThat(result.getId()).isEqualTo(22L);
            assertThat(captor.getValue().getDestination()).isEqualTo("West Lake");
            assertThat(captor.getValue().getDayNo()).isEqualTo(1);
        }
    }

    @Test
    void updatePlanShouldRejectShrinkingBelowExistingItemDay() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 7L, 3));
            when(tripPlanItemMapper.selectList(any())).thenReturn(List.of(item(21L, 11L, 3)));

            TripPlanUpdateRequest request = new TripPlanUpdateRequest();
            request.setPlanName("Shorter");
            request.setTotalDays(2);

            BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> service.updatePlan(11L, request), BusinessException.class);

            assertThat(error.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            verify(tripPlanMapper, never()).updateById(any(TripPlan.class));
        }
    }

    @Test
    void updateAndDeletePlanItemShouldRequireItemBelongingToPlan() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 7L, 3));
            when(tripPlanItemMapper.selectById(21L)).thenReturn(item(21L, 99L, 1));
            TripPlanItemUpdateRequest request = new TripPlanItemUpdateRequest();
            request.setDayNo(1);
            request.setDestination("West Lake");

            BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> service.updatePlanItem(11L, 21L, request), BusinessException.class);

            assertThat(error.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
            verify(tripPlanItemMapper, never()).updateById(any(TripPlanItem.class));
        }
    }

    @Test
    void deletePlanShouldDeleteOwnedItemsBeforePlan() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            when(tripPlanMapper.selectById(11L)).thenReturn(plan(11L, 7L, 3));

            service.deletePlan(11L);

            verify(tripPlanItemMapper).delete(any());
            verify(tripPlanMapper).deleteById(11L);
        }
    }

    private TripPlanCreateRequest createRequest(int days) {
        TripPlanCreateRequest request = new TripPlanCreateRequest();
        request.setPlanName("杭州行程");
        request.setTotalDays(days);
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setRemark(" 备注 ");
        return request;
    }

    private TripPlanItemCreateRequest itemCreateRequest(int dayNo) {
        TripPlanItemCreateRequest request = new TripPlanItemCreateRequest();
        request.setDayNo(dayNo);
        request.setDestination("West Lake");
        request.setHotel("Lake Hotel");
        request.setTransportType("Metro");
        request.setRemark(" 走访 ");
        return request;
    }

    private TripPlan plan(Long id, Long userId, int days) {
        TripPlan plan = new TripPlan();
        plan.setId(id);
        plan.setUserId(userId);
        plan.setPlanName("杭州行程");
        plan.setTotalDays(days);
        plan.setStartDate(LocalDate.of(2026, 9, 1));
        return plan;
    }

    private TripPlanItem item(Long id, Long planId, int dayNo) {
        TripPlanItem item = new TripPlanItem();
        item.setId(id);
        item.setPlanId(planId);
        item.setDayNo(dayNo);
        item.setDestination("West Lake");
        return item;
    }
}
