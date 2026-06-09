package com.travelplatform.service.review.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.review.ReviewCreateRequest;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderHotel;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.User;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.UserMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.review.ReviewVO;
import com.travelplatform.vo.review.ReviewableOrderVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock ReviewMapper reviewMapper;
    @Mock OrdersMapper ordersMapper;
    @Mock UserMapper userMapper;
    @Mock OrderFlightMapper orderFlightMapper;
    @Mock OrderTrainMapper orderTrainMapper;
    @Mock OrderHotelMapper orderHotelMapper;
    @Mock OrderTourMapper orderTourMapper;
    @InjectMocks ReviewServiceImpl service;

    @Test
    void createReviewShouldPersistTrimmedContent() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders order = completedOrder(1L, OrderBizTypeConstant.FLIGHT, 88L);
            when(ordersMapper.selectById(1L)).thenReturn(order);
            when(reviewMapper.selectOne(any())).thenReturn(null);
            when(userMapper.selectById(1L)).thenReturn(user("Demo", "/avatar.png"));

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            when(reviewMapper.insert(captor.capture())).thenAnswer(invocation -> {
                captor.getValue().setId(5L);
                captor.getValue().setCreateTime(LocalDateTime.of(2026, 1, 1, 12, 0));
                return 1;
            });

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setRating(5);
            request.setContent(" Great trip ");

            ReviewVO result = service.createReview(request);

            assertThat(captor.getValue().getContent()).isEqualTo("Great trip");
            assertThat(captor.getValue().getBizType()).isEqualTo(OrderBizTypeConstant.FLIGHT);
            assertThat(result.getAuthorNickname()).isEqualTo("Demo");
            assertThat(result.getId()).isEqualTo(5L);
        }
    }

    @Test
    void createReviewShouldRejectExistingReview() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(ordersMapper.selectById(1L)).thenReturn(completedOrder(1L, OrderBizTypeConstant.FLIGHT, 88L));
            when(reviewMapper.selectOne(any())).thenReturn(new Review());

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setRating(5);
            request.setContent("Great");

            assertThatThrownBy(() -> service.createReview(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void createReviewShouldRejectNonCompletedOrder() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders order = completedOrder(1L, OrderBizTypeConstant.FLIGHT, 88L);
            order.setOrderStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
            when(ordersMapper.selectById(1L)).thenReturn(order);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setOrderId(1L);
            request.setRating(5);
            request.setContent("Great");

            assertThatThrownBy(() -> service.createReview(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void listReviewableOrdersShouldBuildBusinessSummaries() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders flightOrder = completedOrder(1L, OrderBizTypeConstant.FLIGHT, 88L);
            Orders hotelOrder = completedOrder(2L, OrderBizTypeConstant.HOTEL, 99L);
            hotelOrder.setOrderNo("O-2");
            hotelOrder.setTravelDate(LocalDate.of(2026, 2, 2));
            Page<Orders> page = new Page<>(1, 10, 2);
            page.setRecords(List.of(flightOrder, hotelOrder));
            when(ordersMapper.selectPage(any(Page.class), any())).thenReturn(page);
            when(orderFlightMapper.selectList(any())).thenReturn(List.of(flightDetail(1L)));
            when(orderTrainMapper.selectList(any())).thenReturn(List.of());
            when(orderHotelMapper.selectList(any())).thenReturn(List.of(hotelDetail(2L)));
            when(orderTourMapper.selectList(any())).thenReturn(List.of());

            PageResult<ReviewableOrderVO> result = service.listReviewableOrders(1, 10);

            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getRecords().get(0).getSummaryTitle()).isEqualTo("Shanghai -> Beijing");
            assertThat(result.getRecords().get(1).getSummarySubtitle()).contains("Deluxe Room");
        }
    }

    @Test
    void getCurrentUserOrderReviewShouldReturnMappedReview() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(ordersMapper.selectById(1L)).thenReturn(completedOrder(1L, OrderBizTypeConstant.FLIGHT, 88L));
            when(reviewMapper.selectOne(any())).thenReturn(review(7L, 1L, 1L));
            when(userMapper.selectById(1L)).thenReturn(user("", "/avatar.png"));

            ReviewVO result = service.getCurrentUserOrderReview(1L);

            assertThat(result.getContent()).isEqualTo("Great");
            assertThat(result.getAuthorNickname()).isEqualTo("demo");
        }
    }

    @Test
    void getVisibleOrderReviewShouldReturnNullWhenMissing() {
        when(reviewMapper.selectOne(any())).thenReturn(null);

        ReviewVO result = service.getVisibleOrderReview(1L);

        assertThat(result).isNull();
    }

    @Test
    void getVisibleOrderReviewShouldReturnVisibleReview() {
        when(reviewMapper.selectOne(any())).thenReturn(review(7L, 1L, 1L));
        when(userMapper.selectById(1L)).thenReturn(user("Demo", "/avatar.png"));

        ReviewVO result = service.getVisibleOrderReview(1L);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getAuthorAvatar()).isEqualTo("/avatar.png");
    }

    private Orders completedOrder(Long id, String bizType, Long bizId) {
        Orders order = new Orders();
        order.setId(id);
        order.setOrderNo("O-" + id);
        order.setUserId(1L);
        order.setBizType(bizType);
        order.setBizId(bizId);
        order.setOrderStatus(OrderStatusConstant.COMPLETED);
        order.setTravelDate(LocalDate.of(2026, 1, 1));
        return order;
    }

    private Review review(Long id, Long orderId, Long userId) {
        Review review = new Review();
        review.setId(id);
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setBizType(OrderBizTypeConstant.FLIGHT);
        review.setBizId(88L);
        review.setRating(5);
        review.setContent("Great");
        review.setStatus(1);
        review.setCreateTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        return review;
    }

    private User user(String nickname, String avatar) {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    private OrderFlight flightDetail(Long orderId) {
        OrderFlight detail = new OrderFlight();
        detail.setOrderId(orderId);
        detail.setDepartureCity("Shanghai");
        detail.setArrivalCity("Beijing");
        detail.setFlightNo("MU1001");
        detail.setAirlineName("China Eastern");
        return detail;
    }

    private OrderHotel hotelDetail(Long orderId) {
        OrderHotel detail = new OrderHotel();
        detail.setOrderId(orderId);
        detail.setHotelName("Lake Hotel");
        detail.setRoomName("Deluxe Room");
        detail.setCheckInDate(LocalDate.of(2026, 2, 2));
        return detail;
    }
}
