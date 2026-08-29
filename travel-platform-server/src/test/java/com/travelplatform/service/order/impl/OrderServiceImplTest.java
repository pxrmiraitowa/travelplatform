package com.travelplatform.service.order.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.constant.OrderStatusConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.order.FlightOrderCreateRequest;
import com.travelplatform.dto.order.HotelOrderCreateRequest;
import com.travelplatform.dto.order.TourOrderCreateRequest;
import com.travelplatform.dto.order.TrainOrderCreateRequest;
import com.travelplatform.entity.Coupon;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Hotel;
import com.travelplatform.entity.HotelRoom;
import com.travelplatform.entity.OrderFlight;
import com.travelplatform.entity.OrderTrain;
import com.travelplatform.entity.Orders;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.TrainTicket;
import com.travelplatform.entity.TourPackage;
import com.travelplatform.entity.UserContact;
import com.travelplatform.mapper.CouponMapper;
import com.travelplatform.mapper.FlightMapper;
import com.travelplatform.mapper.HotelMapper;
import com.travelplatform.mapper.HotelRoomMapper;
import com.travelplatform.mapper.OrderFlightMapper;
import com.travelplatform.mapper.OrderHotelMapper;
import com.travelplatform.mapper.OrderTourMapper;
import com.travelplatform.mapper.OrderTrainMapper;
import com.travelplatform.mapper.OrdersMapper;
import com.travelplatform.mapper.ReviewMapper;
import com.travelplatform.mapper.TourPackageMapper;
import com.travelplatform.mapper.TrainTicketMapper;
import com.travelplatform.mapper.UserContactMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.order.OrderDetailVO;
import com.travelplatform.vo.order.OrderListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrdersMapper ordersMapper;
    @Mock OrderFlightMapper orderFlightMapper;
    @Mock OrderTrainMapper orderTrainMapper;
    @Mock OrderHotelMapper orderHotelMapper;
    @Mock OrderTourMapper orderTourMapper;
    @Mock FlightMapper flightMapper;
    @Mock TrainTicketMapper trainTicketMapper;
    @Mock HotelMapper hotelMapper;
    @Mock HotelRoomMapper hotelRoomMapper;
    @Mock TourPackageMapper tourPackageMapper;
    @Mock UserContactMapper userContactMapper;
    @Mock CouponMapper couponMapper;
    @Mock ReviewMapper reviewMapper;
    @InjectMocks OrderServiceImpl service;

    @Test
    void createFlightOrderShouldApplyCouponAndReduceStock() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Flight flight = flight();
            UserContact contact = contact();
            Coupon coupon = coupon("FLIGHT", new BigDecimal("50"));
            when(flightMapper.selectById(11L)).thenReturn(flight);
            when(userContactMapper.selectById(21L)).thenReturn(contact);
            when(couponMapper.selectById(31L)).thenReturn(coupon);

            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            when(ordersMapper.insert(orderCaptor.capture())).thenAnswer(invocation -> {
                orderCaptor.getValue().setId(101L);
                return 1;
            });
            ArgumentCaptor<OrderFlight> detailCaptor = ArgumentCaptor.forClass(OrderFlight.class);

            FlightOrderCreateRequest request = new FlightOrderCreateRequest();
            request.setFlightId(11L);
            request.setContactId(21L);
            request.setCouponId(31L);
            request.setRemark("window seat");

            OrderDetailVO result = service.createFlightOrder(request);

            assertThat(orderCaptor.getValue().getOrderNo()).startsWith("FL");
            assertThat(orderCaptor.getValue().getDiscountAmount()).isEqualByComparingTo("50");
            assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo("550");
            verify(orderFlightMapper).insert(detailCaptor.capture());
            assertThat(detailCaptor.getValue().getPassengerName()).isEqualTo("Tom");
            assertThat(flight.getStock()).isEqualTo(4);
            verify(flightMapper).updateById(flight);
            assertThat(result.getFlightInfo().getFlightNo()).isEqualTo("MU1001");
            assertThat(result.getCouponName()).isEqualTo("Flight Coupon");
        }
    }

    @Test
    void createFlightOrderShouldRejectCouponForWrongProductType() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(flightMapper.selectById(11L)).thenReturn(flight());
            when(userContactMapper.selectById(21L)).thenReturn(contact());
            when(couponMapper.selectById(31L)).thenReturn(coupon("HOTEL", new BigDecimal("50")));

            FlightOrderCreateRequest request = new FlightOrderCreateRequest();
            request.setFlightId(11L);
            request.setContactId(21L);
            request.setCouponId(31L);

            assertThatThrownBy(() -> service.createFlightOrder(request)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void createFlightOrderShouldRejectWhenStockIsEmptyAndNotWriteOrder() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Flight flight = flight();
            flight.setStock(0);
            when(flightMapper.selectById(11L)).thenReturn(flight);
            FlightOrderCreateRequest request = new FlightOrderCreateRequest();
            request.setFlightId(11L);
            request.setContactId(21L);

            assertThatThrownBy(() -> service.createFlightOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(400);
            verify(ordersMapper, never()).insert(any(Orders.class));
            verify(userContactMapper, never()).selectById(any(Long.class));
        }
    }

    @Test
    void createTrainOrderShouldRejectEmptySeatStockWithBusinessCode() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            TrainTicket ticket = trainTicket();
            ticket.setFirstClassStock(0);
            when(trainTicketMapper.selectById(12L)).thenReturn(ticket);
            when(userContactMapper.selectById(21L)).thenReturn(contact());
            TrainOrderCreateRequest request = new TrainOrderCreateRequest();
            request.setTrainTicketId(12L);
            request.setContactId(21L);
            request.setSeatType("一等座");

            assertThatThrownBy(() -> service.createTrainOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(400);
            verify(ordersMapper, never()).insert(any(Orders.class));
            verify(trainTicketMapper, never()).updateById(any(TrainTicket.class));
        }
    }

    @Test
    void createHotelOrderShouldRejectEmptyRoomStockWithoutMapperWrites() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(hotelMapper.selectById(51L)).thenReturn(hotel());
            HotelRoom room = hotelRoom();
            room.setStock(0);
            when(hotelRoomMapper.selectById(52L)).thenReturn(room);
            HotelOrderCreateRequest request = new HotelOrderCreateRequest();
            request.setHotelId(51L);
            request.setHotelRoomId(52L);
            request.setContactId(21L);
            request.setCheckInDate(LocalDate.now().plusDays(1));
            request.setCheckOutDate(LocalDate.now().plusDays(2));

            assertThatThrownBy(() -> service.createHotelOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(400);
            verify(ordersMapper, never()).insert(any(Orders.class));
            verify(hotelRoomMapper, never()).updateById(any(HotelRoom.class));
        }
    }

    @Test
    void createTourOrderShouldRejectUnsupportedTravelDateBeforeCheckingStock() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(tourPackageMapper.selectById(61L)).thenReturn(tourPackage());
            TourOrderCreateRequest request = new TourOrderCreateRequest();
            request.setTourPackageId(61L);
            request.setContactId(21L);
            request.setTravelDate(LocalDate.now().plusDays(3));

            assertThatThrownBy(() -> service.createTourOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(400);
            verify(ordersMapper, never()).insert(any(Orders.class));
            verify(userContactMapper, never()).selectById(any(Long.class));
        }
    }

    @Test
    void listCurrentUserOrdersShouldAssembleFlightSummaryAndReviewState() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders order = order(101L, OrderBizTypeConstant.FLIGHT);
            Page<Orders> page = new Page<>(1, 10, 1);
            page.setRecords(List.of(order));
            when(ordersMapper.selectPage(any(Page.class), any())).thenReturn(page);
            when(orderFlightMapper.selectList(any())).thenReturn(List.of(orderFlight(101L)));
            when(orderTrainMapper.selectList(any())).thenReturn(List.of());
            when(orderHotelMapper.selectList(any())).thenReturn(List.of());
            when(orderTourMapper.selectList(any())).thenReturn(List.of());
            when(reviewMapper.selectList(any())).thenReturn(List.of(review(101L)));

            PageResult<OrderListItemVO> result = service.listCurrentUserOrders(OrderBizTypeConstant.FLIGHT, null, 1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getSummaryTitle()).isEqualTo("Shanghai -> Beijing");
            assertThat(result.getRecords().get(0).getSummarySubtitle()).contains("MU1001");
            assertThat(result.getRecords().get(0).getReviewed()).isTrue();
            assertThat(result.getRecords().get(0).getReviewId()).isEqualTo(501L);
        }
    }

    @Test
    void getCurrentUserOrderDetailShouldReturnFlightDetailAndReviewInfo() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(ordersMapper.selectById(101L)).thenReturn(order(101L, OrderBizTypeConstant.FLIGHT));
            when(orderFlightMapper.selectOne(any())).thenReturn(orderFlight(101L));
            when(reviewMapper.selectOne(any())).thenReturn(review(101L));

            OrderDetailVO result = service.getCurrentUserOrderDetail(101L);

            assertThat(result.getFlightInfo()).isNotNull();
            assertThat(result.getTrainInfo()).isNull();
            assertThat(result.getFlightInfo().getDepartureAirport()).isEqualTo("PVG");
            assertThat(result.getReviewInfo().getRating()).isEqualTo(5);
        }
    }

    @Test
    void cancelCurrentUserOrderShouldCancelFlightAndRestoreStock() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders order = order(101L, OrderBizTypeConstant.FLIGHT);
            order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
            OrderFlight detail = orderFlight(101L);
            Flight flight = flight();
            flight.setStock(4);
            when(ordersMapper.selectById(101L)).thenReturn(order);
            when(orderFlightMapper.selectOne(any())).thenReturn(detail);
            when(flightMapper.selectById(11L)).thenReturn(flight);

            service.cancelCurrentUserOrder(101L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatusConstant.CANCELLED);
            assertThat(detail.getStatus()).isEqualTo(OrderStatusConstant.CANCELLED);
            assertThat(flight.getStock()).isEqualTo(5);
            verify(ordersMapper).updateById(order);
            verify(orderFlightMapper).updateById(detail);
            verify(flightMapper).updateById(flight);
        }
    }

    @Test
    void cancelCurrentUserOrderShouldRejectUnsupportedTrainSeatType() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders order = order(102L, OrderBizTypeConstant.TRAIN);
            order.setOrderStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
            OrderTrain detail = orderTrain(102L);
            detail.setSeatType("UNKNOWN");
            TrainTicket ticket = trainTicket();
            when(ordersMapper.selectById(102L)).thenReturn(order);
            when(orderTrainMapper.selectOne(any())).thenReturn(detail);
            when(trainTicketMapper.selectById(12L)).thenReturn(ticket);

            assertThatThrownBy(() -> service.cancelCurrentUserOrder(102L)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void getCurrentUserOrderDetailShouldRejectAnotherUsersOrder() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            Orders otherUsersOrder = order(101L, OrderBizTypeConstant.FLIGHT);
            otherUsersOrder.setUserId(2L);
            when(ordersMapper.selectById(101L)).thenReturn(otherUsersOrder);

            assertThatThrownBy(() -> service.getCurrentUserOrderDetail(101L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
            verify(orderFlightMapper, never()).selectOne(any());
        }
    }

    private Flight flight() {
        Flight flight = new Flight();
        flight.setId(11L);
        flight.setFlightNo("MU1001");
        flight.setAirlineName("China Eastern");
        flight.setDepartureCity("Shanghai");
        flight.setArrivalCity("Beijing");
        flight.setDepartureAirport("PVG");
        flight.setArrivalAirport("PEK");
        flight.setDepartureTime(LocalDateTime.now().plusDays(5));
        flight.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
        flight.setPrice(new BigDecimal("600"));
        flight.setStock(5);
        flight.setStatus(1);
        return flight;
    }

    private UserContact contact() {
        UserContact contact = new UserContact();
        contact.setId(21L);
        contact.setUserId(1L);
        contact.setName("Tom");
        contact.setPhone("13900000000");
        contact.setIdCard("110101199001011234");
        return contact;
    }

    private Coupon coupon(String productType, BigDecimal discount) {
        Coupon coupon = new Coupon();
        coupon.setId(31L);
        coupon.setCouponName("Flight Coupon");
        coupon.setProductType(productType);
        coupon.setThresholdAmount(new BigDecimal("500"));
        coupon.setDiscountAmount(discount);
        coupon.setStatus(1);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(1));
        return coupon;
    }

    private Orders order(Long id, String bizType) {
        Orders order = new Orders();
        order.setId(id);
        order.setOrderNo("ORD-" + id);
        order.setUserId(1L);
        order.setBizType(bizType);
        order.setBizId(OrderBizTypeConstant.TRAIN.equals(bizType) ? 12L : 11L);
        order.setOrderStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setOriginalAmount(new BigDecimal("600"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("600"));
        order.setContactName("Tom");
        order.setContactPhone("13900000000");
        order.setTravelDate(LocalDate.now().plusDays(5));
        order.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return order;
    }

    private OrderFlight orderFlight(Long orderId) {
        OrderFlight detail = new OrderFlight();
        detail.setOrderId(orderId);
        detail.setFlightId(11L);
        detail.setFlightNo("MU1001");
        detail.setAirlineName("China Eastern");
        detail.setDepartureCity("Shanghai");
        detail.setArrivalCity("Beijing");
        detail.setDepartureAirport("PVG");
        detail.setArrivalAirport("PEK");
        detail.setDepartureTime(LocalDateTime.now().plusDays(5));
        detail.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
        detail.setPassengerName("Tom");
        detail.setPassengerPhone("13900000000");
        detail.setPassengerIdCard("110101199001011234");
        detail.setTicketPrice(new BigDecimal("600"));
        detail.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        return detail;
    }

    private OrderTrain orderTrain(Long orderId) {
        OrderTrain detail = new OrderTrain();
        detail.setOrderId(orderId);
        detail.setTrainTicketId(12L);
        detail.setTrainNo("G100");
        detail.setTrainType("G");
        detail.setDepartureCity("Shanghai");
        detail.setArrivalCity("Hangzhou");
        detail.setDepartureStation("Hongqiao");
        detail.setArrivalStation("East");
        detail.setDepartureTime(LocalDateTime.now().plusDays(3));
        detail.setArrivalTime(LocalDateTime.now().plusDays(3).plusHours(1));
        detail.setSeatType("涓€绛夊骇");
        detail.setSeatPrice(new BigDecimal("180"));
        detail.setPassengerName("Tom");
        detail.setPassengerPhone("13900000000");
        detail.setPassengerIdCard("110101199001011234");
        detail.setStatus(OrderStatusConstant.PAID_PENDING_TRAVEL);
        return detail;
    }

    private TrainTicket trainTicket() {
        TrainTicket ticket = new TrainTicket();
        ticket.setId(12L);
        ticket.setTrainNo("G100");
        ticket.setTrainType("G");
        ticket.setDepartureCity("Shanghai");
        ticket.setArrivalCity("Hangzhou");
        ticket.setDepartureStation("Hongqiao");
        ticket.setArrivalStation("East");
        ticket.setDepartureTime(LocalDateTime.now().plusDays(3));
        ticket.setArrivalTime(LocalDateTime.now().plusDays(3).plusHours(1));
        ticket.setFirstClassPrice(new BigDecimal("180"));
        ticket.setFirstClassStock(2);
        ticket.setStatus(1);
        return ticket;
    }

    private Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setId(51L);
        hotel.setHotelName("Test Hotel");
        hotel.setCity("Shanghai");
        hotel.setStatus(1);
        return hotel;
    }

    private HotelRoom hotelRoom() {
        HotelRoom room = new HotelRoom();
        room.setId(52L);
        room.setHotelId(51L);
        room.setRoomName("Standard");
        room.setPrice(new BigDecimal("300"));
        room.setStock(2);
        room.setStatus(1);
        return room;
    }

    private TourPackage tourPackage() {
        TourPackage item = new TourPackage();
        item.setId(61L);
        item.setPackageName("Test Tour");
        item.setTravelDates("2026-09-01,2026-09-02");
        item.setPrice(new BigDecimal("800"));
        item.setStock(2);
        item.setStatus(1);
        return item;
    }

    private Review review(Long orderId) {
        Review review = new Review();
        review.setId(501L);
        review.setOrderId(orderId);
        review.setBizType(OrderBizTypeConstant.FLIGHT);
        review.setBizId(11L);
        review.setRating(5);
        review.setContent("Great");
        review.setCreateTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        return review;
    }
}
